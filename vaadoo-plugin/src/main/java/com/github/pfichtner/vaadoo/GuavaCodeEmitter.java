package com.github.pfichtner.vaadoo;

import static net.bytebuddy.jar.asm.Opcodes.ALOAD;
import static net.bytebuddy.jar.asm.Opcodes.ARRAYLENGTH;
import static net.bytebuddy.jar.asm.Opcodes.DUP;
import static net.bytebuddy.jar.asm.Opcodes.F_SAME;
import static net.bytebuddy.jar.asm.Opcodes.F_SAME1;
import static net.bytebuddy.jar.asm.Opcodes.GETSTATIC;
import static net.bytebuddy.jar.asm.Opcodes.GOTO;
import static net.bytebuddy.jar.asm.Opcodes.I2L;
import static net.bytebuddy.jar.asm.Opcodes.ICONST_0;
import static net.bytebuddy.jar.asm.Opcodes.ICONST_1;
import static net.bytebuddy.jar.asm.Opcodes.IFEQ;
import static net.bytebuddy.jar.asm.Opcodes.IFGE;
import static net.bytebuddy.jar.asm.Opcodes.IFLE;
import static net.bytebuddy.jar.asm.Opcodes.IFLT;
import static net.bytebuddy.jar.asm.Opcodes.IFNONNULL;
import static net.bytebuddy.jar.asm.Opcodes.IFNULL;
import static net.bytebuddy.jar.asm.Opcodes.ILOAD;
import static net.bytebuddy.jar.asm.Opcodes.INTEGER;
import static net.bytebuddy.jar.asm.Opcodes.INVOKEINTERFACE;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESPECIAL;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;
import static net.bytebuddy.jar.asm.Opcodes.INVOKEVIRTUAL;
import static net.bytebuddy.jar.asm.Opcodes.LCMP;
import static net.bytebuddy.jar.asm.Opcodes.LLOAD;
import static net.bytebuddy.jar.asm.Opcodes.NEW;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;

public class GuavaCodeEmitter implements CodeEmitter {

	@Override
	public void addNullCheck(MethodVisitor mv, ParameterInfo parameter) {
		mv.visitVarInsn(ALOAD, parameter.index());
		negated(mv, IFNONNULL);
		mv.visitLdcInsn(parameter.name() + " expected to be null");
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
				"(ZLjava/lang/Object;)V", false);

	}

	@Override
	public void addNotNullCheck(MethodVisitor mv, ParameterInfo parameter) {
		mv.visitVarInsn(ALOAD, parameter.index());
		mv.visitLdcInsn(parameter.name() + " must not be null");
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkNotNull",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
	}

	@Override
	public void addNotEmptyCheck(MethodVisitor mv, ParameterInfo parameter) {
		Class<?> classType;
		// TODO the superType should be passed by the caller (since the caller has the
		// knowledge which type are general ok)
		Class<?> superType;
		if (parameter.isArray()) {
			classType = Object[].class;
			superType = Object[].class;
		} else {
			classType = loadClass(parameter.classname());
			superType = superType(classType, CharSequence.class, Collection.class, Map.class);
		}

		if (superType != null) {
			mv.visitVarInsn(ALOAD, parameter.index());

			boolean isInterface = classType.isInterface();
			int opcode = isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL;
			var internalName = parameter.type().getInternalName();

			if (superType.equals(CharSequence.class)) {
				mv.visitMethodInsn(opcode, internalName, "length", "()I", isInterface);
			} else if (superType.equals(Collection.class)) {
				mv.visitMethodInsn(opcode, internalName, "size", "()I", isInterface);
			} else if (superType.equals(Map.class)) {
				mv.visitMethodInsn(opcode, internalName, "size", "()I", isInterface);
			} else if (superType.equals(Object[].class)) {
				mv.visitInsn(ARRAYLENGTH);
			} else {
				throw new IllegalStateException("Cannot handle type " + classType + ": Unsupported supertype");
			}
			negated(mv, IFLE);
			mv.visitLdcInsn(parameter.name() + " must not be empty");
			mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
					"(ZLjava/lang/Object;)V", false);
		}
	}

	@Override
	public void addNotBlankCheck(MethodVisitor mv, ParameterInfo parameter) {
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/CharMatcher", "whitespace",
				"()Lcom/google/common/base/CharMatcher;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/base/CharMatcher", "negate",
				"()Lcom/google/common/base/CharMatcher;", false);
		mv.visitVarInsn(ALOAD, parameter.index());
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/base/CharMatcher", "countIn",
				"(Ljava/lang/CharSequence;)I", false);
		negated(mv, IFLE);
		mv.visitLdcInsn(parameter.name() + " must not be blank");
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
				"(ZLjava/lang/Object;)V", false);
	}

	@Override
	public void addIsTrueCheck(MethodVisitor mv, ParameterInfo parameter) {
		if (parameter.typeIs(boolean.class)) {
			mv.visitVarInsn(ILOAD, parameter.index());
		} else if (parameter.typeIs(Boolean.class)) {
			mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
			mv.visitVarInsn(ALOAD, parameter.index());
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "equals", "(Ljava/lang/Object;)Z", false);
		}
		mv.visitLdcInsn(parameter.name() + " should be true");
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
				"(ZLjava/lang/Object;)V", false);
	}

	@Override
	public void addIsFalseCheck(MethodVisitor mv, ParameterInfo parameter) {
		if (parameter.typeIs(boolean.class)) {
			mv.visitVarInsn(ILOAD, parameter.index());
			Label label0 = new Label();
			mv.visitJumpInsn(IFEQ, label0);
			mv.visitInsn(ICONST_0);
			Label label1 = new Label();
			mv.visitJumpInsn(GOTO, label1);
			mv.visitLabel(label0);
			mv.visitFrame(F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(label1);
			mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { INTEGER });
		} else if (parameter.typeIs(Boolean.class)) {
			mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
			mv.visitVarInsn(ALOAD, parameter.index());
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "equals", "(Ljava/lang/Object;)Z", false);
		}
		mv.visitLdcInsn(parameter.name() + " should be false");
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
				"(ZLjava/lang/Object;)V", false);
	}

	@Override
	public void addMinCheck(MethodVisitor mv, ParameterInfo parameter) {
		Long minValue = parameter.annotationValue("value").map(String::valueOf).map(Long::valueOf)
				.orElseThrow(() -> new IllegalStateException("Min does not define attribute 'value'"));
		String internalName = parameter.type().getInternalName();

		boolean primitive = parameter.typeIs(long.class) || parameter.typeIs(int.class) || parameter.typeIs(short.class)
				|| parameter.typeIs(byte.class);
		if (primitive) {
			if (parameter.typeIs(long.class)) {
				mv.visitVarInsn(LLOAD, parameter.index());
				mv.visitLdcInsn(minValue);
				mv.visitInsn(LCMP);
			} else if (parameter.typeIs(int.class) || parameter.typeIs(short.class) || parameter.typeIs(byte.class)) {
				mv.visitVarInsn(ILOAD, parameter.index());
				mv.visitInsn(I2L);
				mv.visitLdcInsn(minValue);
				mv.visitInsn(LCMP);
			} else if (parameter.typeIs(Byte.class) || parameter.typeIs(Short.class) //
					|| parameter.typeIs(Integer.class) || parameter.typeIs(Long.class)) {
				mv.visitVarInsn(ALOAD, parameter.index());
				mv.visitMethodInsn(INVOKEVIRTUAL, internalName, "longValue", "()J", false);
				mv.visitLdcInsn(minValue);
				mv.visitInsn(LCMP);
			} else {
				throw new IllegalStateException("Cannot handle type " + parameter.type());
			}
			negated(mv, IFLT);
			mv.visitLdcInsn(parameter.name() + " should be >= " + minValue);
			mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
					"(ZLjava/lang/Object;)V", false);
		} else {
			mv.visitVarInsn(ALOAD, parameter.index());
			Label label0 = new Label();
			mv.visitJumpInsn(IFNULL, label0);
			if (parameter.typeIs(Byte.class) || parameter.typeIs(Short.class) //
					|| parameter.typeIs(Integer.class) || parameter.typeIs(Long.class)) {
				mv.visitVarInsn(ALOAD, parameter.index());
				mv.visitMethodInsn(INVOKEVIRTUAL, internalName, "longValue", "()J", false);
				mv.visitLdcInsn(minValue);
				mv.visitInsn(LCMP);
			} else if (parameter.typeIs(BigDecimal.class)) {
				mv.visitVarInsn(ALOAD, parameter.index());
				mv.visitTypeInsn(NEW, internalName);
				mv.visitInsn(DUP);
				mv.visitLdcInsn(minValue);
				mv.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "(J)V", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, internalName, "compareTo", "(L" + internalName + ";)I", false);
			} else if (parameter.typeIs(BigInteger.class)) {
				mv.visitVarInsn(ALOAD, parameter.index());
				mv.visitLdcInsn(minValue);
				mv.visitMethodInsn(INVOKESTATIC, internalName, "valueOf", "(J)L" + internalName + ";", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, internalName, "compareTo", "(L" + internalName + ";)I", false);
			} else {
				throw new IllegalStateException("Cannot handle type " + parameter.type());
			}

			mv.visitJumpInsn(IFGE, label0);
			mv.visitInsn(ICONST_0);
			Label label1 = new Label();
			mv.visitJumpInsn(GOTO, label1);
			mv.visitLabel(label0);
			mv.visitFrame(F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(label1);
			mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { INTEGER });

			mv.visitLdcInsn(parameter.name() + " should be >= " + minValue);
			mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
					"(ZLjava/lang/Object;)V", false);

		}

	}

	private static void negated(MethodVisitor mv, int opcode) {
		Label label0 = new Label();
		mv.visitJumpInsn(opcode, label0);
		mv.visitInsn(ICONST_1);
		Label label1 = new Label();
		mv.visitJumpInsn(GOTO, label1);
		mv.visitLabel(label0);
		mv.visitFrame(F_SAME, 0, null, 0, null);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(label1);
		mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { INTEGER });
	}

	private boolean isAssignable(String actualClassName, String classToSearch) {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			while (actualClassName != null) {
				if (actualClassName.equals(classToSearch)) {
					return true;
				}
				try (InputStream is = classLoader.getResourceAsStream(actualClassName + ".class")) {
					ClassReader classReader = new ClassReader(is.readAllBytes());
					if (Arrays.asList(classReader.getInterfaces()).contains(classToSearch)) {
						return true;
					}
					actualClassName = classReader.getSuperName();
				}
			}
			return false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SafeVarargs
	private Class<?> superType(Class<?> classToCheck, Class<?>... superTypes) {
		return Arrays.stream(superTypes).filter(t -> t.isAssignableFrom(classToCheck)).findFirst().orElse(null);
	}

	public static Class<?> loadClass(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
