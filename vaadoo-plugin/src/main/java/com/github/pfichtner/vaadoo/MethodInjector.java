package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.AsmUtil.isArray;
import static com.github.pfichtner.vaadoo.AsmUtil.isLoadOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isReturnOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isStoreOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.sizeOf;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static net.bytebuddy.jar.asm.Opcodes.AASTORE;
import static net.bytebuddy.jar.asm.Opcodes.ACC_STATIC;
import static net.bytebuddy.jar.asm.Opcodes.ANEWARRAY;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Opcodes.BIPUSH;
import static net.bytebuddy.jar.asm.Opcodes.DUP;
import static net.bytebuddy.jar.asm.Opcodes.GETSTATIC;
import static net.bytebuddy.jar.asm.Type.LONG_TYPE;
import static net.bytebuddy.jar.asm.Type.getArgumentTypes;
import static net.bytebuddy.jar.asm.Type.getMethodDescriptor;
import static net.bytebuddy.jar.asm.Type.getObjectType;
import static net.bytebuddy.jar.asm.Type.getReturnType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map.Entry;
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

		private final String sourceMethodName;
		private final String searchDescriptor;
		private final MethodVisitor targetMethodVisitor;
		private final ParameterInfo parameter;
		private final int offset;
		private final int firstLocal;

		private MethodInjectorClassVisitor(int api, Method sourceMethod, MethodVisitor targetMethodVisitor,
				ParameterInfo parameter) {
			super(api);
			this.sourceMethodName = sourceMethod.getName();
			this.searchDescriptor = getMethodDescriptor(sourceMethod);
			this.targetMethodVisitor = targetMethodVisitor;
			this.parameter = parameter;
			this.offset = isStatic(sourceMethod.getModifiers()) ? 0 : 1;
			this.firstLocal = offset
					+ sizeOf(stream(sourceMethod.getParameterTypes()).map(Type::getType).toArray(Type[]::new));
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
				String[] exceptions) {

			if (name.equals(sourceMethodName) && descriptor.equals(searchDescriptor)) {
				// TODO migrate to LocalVariablesSorter
				return new MethodVisitor(ASM9, targetMethodVisitor) {

					private boolean firstParamLoadStart;
					private Type handledAnnotation;

					private final Function<String, String> rbResolver = Resources::message;
					private final Function<String, String> paramNameResolver = k -> k.equals(NAME) ? parameter.name()
							: k;
					private final Function<String, Object> annotationValueResolver = k -> parameter.annotationValues()
							.getOrDefault(k, k);
					private final Function<String, Object> resolver = rbResolver.andThen(paramNameResolver)
							.andThen(annotationValueResolver);

					// source has e.g. 2 params, target has e.g. 8, so we would have to add 6 to
					// each local variable access
					private int localVarOffset = MethodInjectorClassVisitor.this.firstLocal
							- ((ACC_STATIC & access) == 0 ? 1 : 0 + sizeOf(getArgumentTypes(descriptor)));

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
						if (var >= MethodInjectorClassVisitor.this.firstLocal
								&& (isLoadOpcode(opcode) || isStoreOpcode(opcode))) {
							super.visitVarInsn(opcode, var + localVarOffset);
						} else {
							if (isLoadOpcode(opcode) && var == 0 + offset) {
								firstParamLoadStart = true;
							} else {
								super.visitVarInsn(opcode, adjustOpcode(opcode, var, parameter));
							}
						}
					}

					@Override
					public void visitIincInsn(int varIndex, int increment) {
						super.visitIincInsn(varIndex + localVarOffset, increment);
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
							var returnType = getReturnType(descriptor);
							if (isArray(returnType)) {
								writeArray(returnType.getElementType(),
										parameter.arrayValues().getOrDefault(name, emptyMap()).entrySet());
							} else {
								visitLdcInsn(annotationsLdcInsnValue(parameter, owner, name, returnType));
							}

							firstParamLoadStart = false;
						} else {
							super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
						}
					}

					private void writeArray(Type arrayElementType, Collection<Entry<Type, String>> entries) {
						mv.visitIntInsn(BIPUSH, entries.size());
						// TODO this only works for objects but not primitive arrays
						mv.visitTypeInsn(ANEWARRAY, arrayElementType.getInternalName());

						int idx = 0;
						for (Entry<Type, String> next : entries) {
							mv.visitInsn(DUP);
							mv.visitIntInsn(BIPUSH, idx++);
							mv.visitFieldInsn(GETSTATIC, next.getKey().getInternalName(), next.getValue(),
									next.getKey().getDescriptor());
							mv.visitInsn(AASTORE);
						}
					}

					private Object annotationsLdcInsnValue(ParameterInfo parameter, String owner, String name,
							Type returnType) {
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

	public void inject(MethodVisitor targetMethodVisitor, ParameterInfo parameter, Method sourceMethod) {
		this.classReader.accept(new MethodInjectorClassVisitor(ASM9, sourceMethod, targetMethodVisitor, parameter), 0);
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
