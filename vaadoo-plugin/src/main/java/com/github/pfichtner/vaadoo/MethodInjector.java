package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.AsmUtil.isLoadOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isReturnOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isStoreOpcode;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Type.LONG_TYPE;
import static net.bytebuddy.jar.asm.Type.getMethodDescriptor;
import static net.bytebuddy.jar.asm.Type.getObjectType;
import static net.bytebuddy.jar.asm.Type.getReturnType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;

public class MethodInjector {

	private static final class MethodInjectorClassVisitor extends ClassVisitor {

		private final Method sourceMethod;
		private final MethodVisitor mv;
		private final ParameterInfo parameter;
		private final int offset;

		private MethodInjectorClassVisitor(int api, Method sourceMethod, MethodVisitor mv, ParameterInfo parameter,
				int offset) {
			super(api);
			this.sourceMethod = sourceMethod;
			this.mv = mv;
			this.parameter = parameter;
			this.offset = offset;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
				String[] exceptions) {

			String searchDescriptor = getMethodDescriptor(sourceMethod);

			if (name.equals(sourceMethod.getName()) && descriptor.equals(searchDescriptor)) {
				return new MethodVisitor(ASM9, mv) {

					private boolean firstParamLoadStart;
					private Type handledAnnotation;

					private final Function<String, String> rbResolver = Resources::message;
					private final Function<String, String> paramNameResolver = k -> k.equals(NAME) ? parameter.name()
							: k;
					private final Function<String, Object> annotationValueResolver = k -> parameter.annotationValues()
							.getOrDefault(k, k);
					private final Function<String, Object> resolver = rbResolver.andThen(paramNameResolver)
							.andThen(annotationValueResolver);

					@Override
					public void visitLineNumber(int line, Label start) {
						// ignore
					}

					public void visitLocalVariable(String name, String descriptor, String signature, Label start,
							Label end, int index) {
						// ignore, we would have to rewrite owner
					}

					@Override
					public void visitMaxs(int maxStack, int maxLocals) {
						// ignore
					}

					@Override
					public void visitVarInsn(int opcode, int var) {
						if (isLoadOpcode(opcode) && var == 0 + offset) {
							firstParamLoadStart = true;
						} else {
							super.visitVarInsn(opcode, adjustOpcode(opcode, var, parameter));
						}
					}

					private int adjustOpcode(int opcode, int var, ParameterInfo parameter) {
						return isLoadOpcode(opcode) || isStoreOpcode(opcode) //
								? var + parameter.index() - 1 - offset
								: var;
					}

					public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
							boolean isInterface) {
						this.handledAnnotation = getObjectType(owner);
						if (firstParamLoadStart) {
							visitLdcInsn(annotationsLdcInsnValue(parameter, owner, name, descriptor));
							firstParamLoadStart = false;
						} else {
							super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
						}
					}

					private Object annotationsLdcInsnValue(ParameterInfo parameter, String owner, String name,
							String descriptor) {
						var returnType = getReturnType(descriptor);
						var valueFromClass = parameter.annotationValue(name).map(String::valueOf);
						if (LONG_TYPE.equals(returnType)) {
							return valueFromClass
									.or(() -> Optional.of(defaultValue(this.handledAnnotation.getClassName(), name)))
									.map(Long::valueOf).orElseThrow(() -> new IllegalStateException(
											format("'%s' does not define attribute '%s'", owner, name)));
						} else if (Type.getType(String.class).equals(returnType)) {
							return valueFromClass
									.or(() -> Optional.of(defaultValue(this.handledAnnotation.getClassName(), name)))
									.orElseThrow(() -> new IllegalStateException(
											format("'%s' does not define attribute '%s'", owner, name)));
						}
						throw new IllegalStateException("Unsupported type " + returnType);
					};

					@Override
					public void visitInsn(int opcode) {
						if (!isReturnOpcode(opcode)) {
							super.visitInsn(opcode);
						}
					}

					@Override
					public void visitLdcInsn(Object value) {
						super.visitLdcInsn(value instanceof String //
								? NamedPlaceholders.replace((String) value, resolver) //
								: value);
					}

					public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
						if ("makeConcatWithConstants".equals(name) && "(J)Ljava/lang/String;".equals(descriptor)
								&& "makeConcatWithConstants".equals(handle.getName())
								&& "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
										.equals(handle.getDesc())
								&& "java/lang/invoke/StringConcatFactory".equals(handle.getOwner()) && args.length >= 0
								&& args[0] instanceof String) {
							args[0] = format((String) args[0], parameter.name());
						}
						super.visitInvokeDynamicInsn(name, descriptor, handle, args);
					};

				};
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}

	private final ClassReader classReader;

	private static final String NAME = "@@@NAME@@@";

	public MethodInjector(Class<? extends Jsr380CodeFragment> clazz) {
		String className = clazz.getName().replace('.', '/') + ".class";

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(className)) {
			if (inputStream == null) {
				throw new IllegalStateException("Class " + clazz.getName() + " not found on classpath.");
			}

			this.classReader = new ClassReader(inputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void inject(MethodVisitor mv, ParameterInfo parameter, Method sourceMethod) {
		int offset = isStatic(sourceMethod.getModifiers()) ? 0 : 1;

		this.classReader.accept(new MethodInjectorClassVisitor(ASM9, sourceMethod, mv, parameter, offset), 0);
	}

	private static String defaultValue(String className, String name) {
		try {
			return defaultValue(Class.forName(className), name);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String defaultValue(Class<?> clazz, String name) {
		return getDefaultValue(clazz, name);
	}

	private static String getDefaultValue(Class<?> clazz, String name) {
		return stream(clazz //
				.getMethods()) //
				.filter(m -> name.equals(m.getName())) //
				.findFirst() //
				.map(Method::getDefaultValue) //
				.map(String::valueOf) //
				.orElse(null);
	}

}
