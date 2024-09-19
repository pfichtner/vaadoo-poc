package com.github.pfichtner.vaadoo;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static net.bytebuddy.jar.asm.Opcodes.ALOAD;
import static net.bytebuddy.jar.asm.Opcodes.ARETURN;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Opcodes.ASTORE;
import static net.bytebuddy.jar.asm.Opcodes.DLOAD;
import static net.bytebuddy.jar.asm.Opcodes.DRETURN;
import static net.bytebuddy.jar.asm.Opcodes.DSTORE;
import static net.bytebuddy.jar.asm.Opcodes.FLOAD;
import static net.bytebuddy.jar.asm.Opcodes.FRETURN;
import static net.bytebuddy.jar.asm.Opcodes.FSTORE;
import static net.bytebuddy.jar.asm.Opcodes.ILOAD;
import static net.bytebuddy.jar.asm.Opcodes.IRETURN;
import static net.bytebuddy.jar.asm.Opcodes.ISTORE;
import static net.bytebuddy.jar.asm.Opcodes.LLOAD;
import static net.bytebuddy.jar.asm.Opcodes.LRETURN;
import static net.bytebuddy.jar.asm.Opcodes.LSTORE;
import static net.bytebuddy.jar.asm.Opcodes.RETURN;
import static net.bytebuddy.jar.asm.Type.LONG_TYPE;
import static net.bytebuddy.jar.asm.Type.getMethodDescriptor;
import static net.bytebuddy.jar.asm.Type.getObjectType;
import static net.bytebuddy.jar.asm.Type.getReturnType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
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

	private final Class<? extends Jsr380CodeFragment> clazz;
	private final ClassReader classReader;

	public MethodInjector(Class<? extends Jsr380CodeFragment> clazz) {
		this.clazz = clazz;
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

	public void injectCheck(MethodVisitor mv, ParameterInfo parameter, Class<?>... parameters) {
		inject(mv, parameter, "check", parameters);
	}

	public void inject(MethodVisitor mv, ParameterInfo parameter, String methodName, Class<?>... parameters) {
		Method sourceMethod = findMethod(methodName, parameters);
		int offset = isStatic(sourceMethod.getModifiers()) ? 0 : 1;

		this.classReader.accept(new ClassVisitor(ASM9) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {

				String searchDescriptor = getMethodDescriptor(sourceMethod);

				if (name.equals(methodName) && descriptor.equals(searchDescriptor)) {
					return new MethodVisitor(ASM9, mv) {

						private final Map<String, String> fallbackMessages = Map.of( //
								unquote(defaultValue(Null.class, "message")), "{@@@name@@@} expected to be null", //
								unquote(defaultValue(NotNull.class, "message")), "{@@@name@@@} must not be null", //
								unquote(defaultValue(NotBlank.class, "message")), "{@@@name@@@} must not be blank", //
								unquote(defaultValue(NotEmpty.class, "message")), "{@@@name@@@} must not be empty", //
								unquote(defaultValue(AssertTrue.class, "message")), "{@@@name@@@} should be true", //
								unquote(defaultValue(AssertFalse.class, "message")), "{@@@name@@@} should be false", //
								unquote(defaultValue(Min.class, "message")), "{@@@name@@@} should be >= {value}" //
						);

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

						private String unquote(String value) {
							Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(value);
							return matcher.find() ? matcher.group(1) : value;

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
									? var + parameter.index() - 1 - offset //
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
								return valueFromClass.or(
										() -> Optional.of(defaultValue(this.handledAnnotation.getClassName(), name)))
										.map(Long::valueOf).orElseThrow(() -> new IllegalStateException(
												format("'%s' does not define attribute '%s'", owner, name)));
							} else if (Type.getType(String.class).equals(returnType)) {
								return valueFromClass.or(
										() -> Optional.of(defaultValue(this.handledAnnotation.getClassName(), name)))
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

						private boolean isLoadOpcode(int opcode) {
							return opcode == ALOAD || opcode == ILOAD || opcode == LLOAD //
									|| opcode == FLOAD || opcode == DLOAD;
						}

						private boolean isStoreOpcode(int opcode) {
							return opcode == ASTORE || opcode == ISTORE || opcode == LSTORE //
									|| opcode == FSTORE || opcode == DSTORE;
						}

						private boolean isReturnOpcode(int opcode) {
							return opcode == RETURN || opcode == ARETURN || opcode == IRETURN //
									|| opcode == LRETURN || opcode == FRETURN || opcode == DRETURN;
						}

						@Override
						public void visitLdcInsn(Object value) {
							super.visitLdcInsn(value instanceof String //
									? formatMessage(parameter, (String) value) //
									: value);
						}

						private String formatMessage(ParameterInfo parameter, String value) {
							Map<String, Object> mutable = new HashMap<>(fallbackMessages);
							mutable.put("@@@name@@@", parameter.name());
							mutable.putAll(parameter.annotationValues());
							return replaceNamedPlaceholders(value, mutable);
						}

						private String replaceNamedPlaceholders(String template, Map<String, Object> replacements) {
							Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
							String result = template;
							while (true) {
								Matcher matcher = pattern.matcher(result);
								StringBuffer sb = new StringBuffer();
								boolean replaced = false;
								while (matcher.find()) {
									String key = matcher.group(1);
									String newValue = "{" + key + "}";
									String replacement = replacements.getOrDefault(key, newValue).toString();
									if (!replacement.equals(newValue)) {
										replaced = true;
										matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
									}
								}
								matcher.appendTail(sb);
								if (!replaced) {
									break;
								}
								result = sb.toString();
							}
							return result;
						}

						private String defaultValue(String className, String name) {
							try {
								return defaultValue(Class.forName(className), name);
							} catch (ClassNotFoundException e) {
								throw new IllegalStateException(e);
							}
						}

						private String defaultValue(Class<?> clazz, String name) {
							return getDefaultValue(clazz, name);
						}

						private String getDefaultValue(Class<?> clazz, String name) {
							return stream(clazz //
									.getMethods()) //
									.filter(m -> name.equals(m.getName())) //
									.findFirst() //
									.map(Method::getDefaultValue) //
									.map(String::valueOf) //
									.orElse(null);
						}

						public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle,
								Object... args) {
							if ("makeConcatWithConstants".equals(name) && "(J)Ljava/lang/String;".equals(descriptor)
									&& "makeConcatWithConstants".equals(handle.getName())
									&& "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
											.equals(handle.getDesc())
									&& "java/lang/invoke/StringConcatFactory".equals(handle.getOwner())
									&& args.length >= 0 && args[0] instanceof String) {
								args[0] = format((String) args[0], parameter.name());
							}
							super.visitInvokeDynamicInsn(name, descriptor, handle, args);
						};

					};
				}
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
		}, 0);
	}

	private Method findMethod(String methodName, Class<?>... parameters) {
		try {
			return clazz.getMethod(methodName, parameters);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

}
