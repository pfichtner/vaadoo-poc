package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.AsmUtil.isLoadOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isReturnOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isStoreOpcode;
import static com.github.pfichtner.vaadoo.NamedPlaceholders.quote;
import static com.github.pfichtner.vaadoo.NamedPlaceholders.unquote;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.Map.entry;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Type.LONG_TYPE;
import static net.bytebuddy.jar.asm.Type.getMethodDescriptor;
import static net.bytebuddy.jar.asm.Type.getObjectType;
import static net.bytebuddy.jar.asm.Type.getReturnType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
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

					Function<String, String> paramNameResolver = k -> k.equals(QUOTED_NAME) ? parameter.name() : k;
					Function<String, Object> annotationValueResolver = k -> parameter.annotationValues()
							.getOrDefault(unquote(k), k);
					Function<String, Object> resolver = defaultMessageResolver.andThen(paramNameResolver)
							.andThen(annotationValueResolver);

					private boolean firstParamLoadStart;
					private Type handledAnnotation;

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

	private static final String NAME = "{@@@name@@@}";
	private static final String QUOTED_NAME = quote("@@@name@@@");

	private final static Map<String, String> defaultMessages = Map.ofEntries( //
			entry(message(Null.class), format("%s must be null", NAME)), //
			entry(message(NotNull.class), format("%s must not be null", NAME)), //
			entry(message(NotBlank.class), format("%s must not be blank", NAME)), //
			entry(message(NotEmpty.class), format("%s must not be empty", NAME)), //
			entry(message(AssertTrue.class), format("%s must be true", NAME)), //
			entry(message(AssertFalse.class), format("%s must be false", NAME)), //
			entry(message(Min.class), format("%s must be greater than or equal to {value}", NAME)), //
			entry(message(Max.class), format("%s must be less than or equal to {value}", NAME)) //

//			entry(message(DecimalMax.class), format("%s must be less than ${inclusive == true ? 'or equal to ' : ''}{value}", NAME)), //
//			entry(message(DecimalMin.class), format("%s must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}", NAME)), //
//			entry(message(Digits.class), format("%s numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)", NAME)), //
//			entry(message(Email.class), format("%s must be a well-formed email address", NAME)), //
//			entry(message(Future.class), format("%s must be a future date", NAME)), //
//			entry(message(FutureOrPresent.class), format("%s must be a date in the present or in the future", NAME)), //
//			entry(message(Negative.class), format("%s must be less than 0", NAME)), //
//			entry(message(NegativeOrZero.class), format("%s must be less than or equal to 0", NAME)), //
//			entry(message(Past.class), format("%s must be a past date", NAME)), //
//			entry(message(PastOrPresent.class), format("%s must be a date in the past or in the present", NAME)), //
//			entry(message(Pattern.class), format("%s must match \"{regexp}\"", NAME)), //
//			entry(message(Positive.class), format("%s must be greater than 0", NAME)), //
//			entry(message(PositiveOrZero.class), format("%s must be greater than or equal to 0", NAME)), //
//			entry(message(Size.class), format("%s size must be between {min} and {max}", NAME)) //
	);
	private static final Function<String, String> defaultMessageResolver = k -> defaultMessages.getOrDefault(k, k);

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

	private static String message(Class<?> clazz) {
		return getDefaultValue(clazz, "message");
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
