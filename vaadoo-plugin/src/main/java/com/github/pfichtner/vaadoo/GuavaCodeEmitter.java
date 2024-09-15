package com.github.pfichtner.vaadoo;

import static net.bytebuddy.jar.asm.Opcodes.ALOAD;
import static net.bytebuddy.jar.asm.Opcodes.ARRAYLENGTH;
import static net.bytebuddy.jar.asm.Opcodes.BIPUSH;
import static net.bytebuddy.jar.asm.Opcodes.F_SAME;
import static net.bytebuddy.jar.asm.Opcodes.F_SAME1;
import static net.bytebuddy.jar.asm.Opcodes.GETSTATIC;
import static net.bytebuddy.jar.asm.Opcodes.GOTO;
import static net.bytebuddy.jar.asm.Opcodes.ICONST_0;
import static net.bytebuddy.jar.asm.Opcodes.ICONST_1;
import static net.bytebuddy.jar.asm.Opcodes.IFEQ;
import static net.bytebuddy.jar.asm.Opcodes.IFLE;
import static net.bytebuddy.jar.asm.Opcodes.IFNONNULL;
import static net.bytebuddy.jar.asm.Opcodes.IF_ICMPLT;
import static net.bytebuddy.jar.asm.Opcodes.ILOAD;
import static net.bytebuddy.jar.asm.Opcodes.INTEGER;
import static net.bytebuddy.jar.asm.Opcodes.INVOKEINTERFACE;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;
import static net.bytebuddy.jar.asm.Opcodes.INVOKEVIRTUAL;

import java.io.InputStream;
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
		var superType = parameter.isArray() //
				? Object[].class //
				: superType(parameter.classname(), CharSequence.class, Collection.class, Map.class);
		if (superType != null) {
			mv.visitVarInsn(ALOAD, parameter.index());
			if (superType.equals(CharSequence.class)) {
				mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "length", "()I", true);
			} else if (superType.equals(Collection.class)) {
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "size", "()I", true);
			} else if (superType.equals(Map.class)) {
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "size", "()I", true);
			} else if (superType.equals(Object[].class)) {
				mv.visitInsn(ARRAYLENGTH);
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
		int min = parameter.annotationValue("value").map(String::valueOf).map(Integer::parseInt).orElse(0);
		// this is load integer argument x
		mv.visitVarInsn(ILOAD, parameter.index());
		mv.visitIntInsn(BIPUSH, min);
		negated(mv, IF_ICMPLT);
		mv.visitLdcInsn(parameter.name() + " should be >= " + min);
		mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkArgument",
				"(ZLjava/lang/Object;)V", false);

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
	private Class<?> superType(String className, Class<?>... superTypes) {
		return Arrays.stream(superTypes).filter(t -> isAssignable(className, t)).findFirst().orElse(null);
	}

	private static boolean isAssignable(String className, Class<?> superType) {
		try {
			return superType.isAssignableFrom(Class.forName(className));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
