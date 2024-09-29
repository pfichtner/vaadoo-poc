package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.AsmUtil.classReader;
import static com.github.pfichtner.vaadoo.AsmUtil.isArray;
import static com.github.pfichtner.vaadoo.AsmUtil.isLoadOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isReturnOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.isStoreOpcode;
import static com.github.pfichtner.vaadoo.AsmUtil.sizeOf;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static net.bytebuddy.jar.asm.Opcodes.AALOAD;
import static net.bytebuddy.jar.asm.Opcodes.AASTORE;
import static net.bytebuddy.jar.asm.Opcodes.ANEWARRAY;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Opcodes.ASTORE;
import static net.bytebuddy.jar.asm.Opcodes.BIPUSH;
import static net.bytebuddy.jar.asm.Opcodes.DUP;
import static net.bytebuddy.jar.asm.Opcodes.GETSTATIC;
import static net.bytebuddy.jar.asm.Opcodes.SIPUSH;
import static net.bytebuddy.jar.asm.Type.INT_TYPE;
import static net.bytebuddy.jar.asm.Type.LONG_TYPE;
import static net.bytebuddy.jar.asm.Type.getArgumentTypes;
import static net.bytebuddy.jar.asm.Type.getMethodDescriptor;
import static net.bytebuddy.jar.asm.Type.getObjectType;
import static net.bytebuddy.jar.asm.Type.getReturnType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Function;

import com.github.pfichtner.vaadoo.ParameterInfo.EnumEntry;
import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;

public class MethodInjector {

	// we remove the first arg (the code inserted has the annotation as it's first
	// argument)
	private static final int REMOVED_PARAMETERS = 1;
	private static final boolean TARGET_METHOD_IS_STATIC = true;

	private static final class MethodInjectorClassVisitor extends ClassVisitor {

		private final String sourceMethodOwner;
		private final String sourceMethodName;
		private final String searchDescriptor;
		private final MethodVisitor targetMethodVisitor;
		private final ParameterInfo parameter;

		private int sourceFirstArgAt;
		private int sourceFirstLocalAt;
		private int argOffset;
		private int localOffset;

		private MethodInjectorClassVisitor(int api, Method sourceMethod, MethodVisitor targetMethodVisitor,
				String signatureOfTargetMethod, ParameterInfo parameter) {
			super(api);
			this.sourceMethodOwner = Type.getType(sourceMethod.getDeclaringClass()).getInternalName();
			this.sourceMethodName = sourceMethod.getName();
			this.searchDescriptor = getMethodDescriptor(sourceMethod);
			this.targetMethodVisitor = targetMethodVisitor;
			this.parameter = parameter;

			this.sourceFirstArgAt = Modifier.isStatic(sourceMethod.getModifiers()) ? 0 : 1;
			this.sourceFirstLocalAt = sourceFirstArgAt
					+ sizeOf(stream(sourceMethod.getParameterTypes()).map(Type::getType).toArray(Type[]::new));

			int targetFirstArgAt = TARGET_METHOD_IS_STATIC ? 0 : 1;
			int targetFirstLocalAt = targetFirstArgAt + sizeOf(getArgumentTypes(signatureOfTargetMethod));

			this.argOffset = sourceFirstArgAt - targetFirstArgAt;
			this.localOffset = sourceFirstLocalAt - targetFirstLocalAt;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
				String[] exceptions) {

			if (name.equals(sourceMethodName) && descriptor.equals(searchDescriptor)) {
				// TODO migrate to LocalVariablesSorter
				return new MethodVisitor(ASM9, targetMethodVisitor) {

					private final boolean isStatic = isStatic(access);

					private boolean firstParamLoadStart;
					private Type handledAnnotation;

					private final Function<String, String> rbResolver = Resources::message;
					private final Function<String, String> paramNameResolver = k -> k.equals(NAME) ? parameter.name()
							: k;
					private final Function<String, Object> annotationValueResolver = k -> {
						var annotationValue = parameter.annotationValue(handledAnnotation, k);
						return annotationValue == null ? k : annotationValue;
					};
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
					public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						if (!owner.startsWith("java/lang")) {
							throw new IllegalStateException(format(
									"code that gets inserted must not access fields, found access to %s#%s in %s",
									owner, name, sourceMethodOwner));
						}
					}

					@Override
					public void visitMaxs(int maxStack, int maxLocals) {
						// ignore
					}

					@Override
					public void visitVarInsn(int opcode, int var) {
						boolean opcodeIsLoad = isLoadOpcode(opcode);
						boolean opcodeIsStore = isStoreOpcode(opcode);

						if (opcodeIsLoad || opcodeIsStore) {
							if (var >= sourceFirstLocalAt) {
								super.visitVarInsn(opcode, remapLocal(var));
							} else {
								if (opcodeIsLoad && var == sourceFirstArgAt) {
									firstParamLoadStart = true;
								} else if (isArrayHandlingCase(opcode, var)) {
									if (!opcodeIsStore) {
										super.visitVarInsn(opcode, remapArg(var));
									}
								} else {
									super.visitVarInsn(opcode, remapArg(var));
								}
							}
						} else {
							super.visitVarInsn(opcode, var);
						}
					}

					private boolean isArrayHandlingCase(int opcode, int var) {
						return opcode == AALOAD || opcode == ASTORE;
					}

					private int remapArg(int var) {
						return var + parameter.index() - argOffset - REMOVED_PARAMETERS;
					}

					private int remapLocal(int varIndex) {
						return varIndex - localOffset;
					}

					@Override
					public void visitIincInsn(int varIndex, int increment) {
						super.visitIincInsn(remapLocal(varIndex), increment);
					}

					public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
							boolean isInterface) {
						if (owner.equals(sourceMethodOwner)) {
							throw new IllegalStateException(format(
									"code that gets inserted must not access methods in the class inserted, found access to %s#%s in %s",
									owner, name, sourceMethodOwner));
						}

						this.handledAnnotation = getObjectType(owner);
						if (firstParamLoadStart) {
							var returnType = getReturnType(descriptor);
							if (isArray(returnType)) {
								@SuppressWarnings("unchecked")
								var annotationValues = (List<EnumEntry>) parameter.annotationValue(handledAnnotation,
										name);
								writeArray(returnType.getElementType(),
										annotationValues == null ? emptyList() : annotationValues);
							} else {
								visitLdcInsn(annotationsLdcInsnValue(parameter, owner, name, returnType));
							}

							firstParamLoadStart = false;
						} else {
							super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
						}
					}

					private void writeArray(Type arrayElementType, List<EnumEntry> annotationValues) {
						var intInsn = annotationValues.size() <= 127 ? BIPUSH : SIPUSH;
						mv.visitIntInsn(intInsn, annotationValues.size());
						// TODO this only works for objects but not primitive arrays
						mv.visitTypeInsn(ANEWARRAY, arrayElementType.getInternalName());

						int idx = 0;
						for (EnumEntry entry : annotationValues) {
							mv.visitInsn(DUP);
							mv.visitIntInsn(intInsn, idx++);
							mv.visitFieldInsn(GETSTATIC, entry.type().getInternalName(), entry.value(),
									entry.type().getDescriptor());
							mv.visitInsn(AASTORE);
						}
					}

					private Object annotationsLdcInsnValue(ParameterInfo parameter, String owner, String name,
							Type returnType) {
						if (LONG_TYPE.equals(returnType)) {
							return Long.valueOf(String.valueOf(valueFromClass(parameter, owner, name)));
						} else if (INT_TYPE.equals(returnType)) {
							return Integer.valueOf(String.valueOf(valueFromClass(parameter, owner, name)));
						} else if (Type.getType(String.class).equals(returnType)) {
							return String.valueOf(valueFromClass(parameter, owner, name));
						}
						throw new IllegalStateException("Unsupported type " + returnType);
					}

					private Object valueFromClass(ParameterInfo parameter, String owner, String name) {
						var valueFromClass = parameter.annotationValue(handledAnnotation, name);
						if (valueFromClass != null) {
							return valueFromClass;
						}
						var defaultValue = defaultValue(this.handledAnnotation.getClassName(), name);
						if (defaultValue != null) {
							return defaultValue;
						}
						throw new IllegalStateException(format("'%s' does not define attribute '%s'", owner, name));
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
	private final String signatureOfTargetMethod;

	public MethodInjector(Class<? extends Jsr380CodeFragment> clazz, String signatureOfTargetMethod) {
		this.classReader = classReader(clazz);
		this.signatureOfTargetMethod = signatureOfTargetMethod;
	}

	public void inject(MethodVisitor targetMethodVisitor, ParameterInfo parameter, Method sourceMethod) {
		this.classReader.accept(new MethodInjectorClassVisitor(ASM9, sourceMethod, targetMethodVisitor,
				signatureOfTargetMethod, parameter), 0);
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
