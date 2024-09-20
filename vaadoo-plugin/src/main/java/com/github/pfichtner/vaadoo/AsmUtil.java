package com.github.pfichtner.vaadoo;

import static java.lang.reflect.Array.newInstance;
import static net.bytebuddy.jar.asm.Opcodes.ALOAD;
import static net.bytebuddy.jar.asm.Opcodes.ARETURN;
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
import static net.bytebuddy.jar.asm.Type.ARRAY;

import net.bytebuddy.jar.asm.Type;

public final class AsmUtil {

	private AsmUtil() {
		super();
	}

	public static boolean isLoadOpcode(int opcode) {
		return opcode == ALOAD || opcode == ILOAD || opcode == LLOAD //
				|| opcode == FLOAD || opcode == DLOAD;
	}

	public static boolean isStoreOpcode(int opcode) {
		return opcode == ASTORE || opcode == ISTORE || opcode == LSTORE //
				|| opcode == FSTORE || opcode == DSTORE;
	}

	public static boolean isReturnOpcode(int opcode) {
		return opcode == RETURN || opcode == ARETURN || opcode == IRETURN //
				|| opcode == LRETURN || opcode == FRETURN || opcode == DRETURN;
	}

	public static Class<?> classtype(Type type) {
		try {
			switch (type.getSort()) {
			case Type.BOOLEAN:
				return boolean.class;
			case Type.CHAR:
				return char.class;
			case Type.BYTE:
				return byte.class;
			case Type.SHORT:
				return short.class;
			case Type.INT:
				return int.class;
			case Type.FLOAT:
				return float.class;
			case Type.LONG:
				return long.class;
			case Type.DOUBLE:
				return double.class;
			case Type.VOID:
				return void.class;
			case Type.ARRAY:
				var elementType = type.getElementType();
				return newInstance(Class.forName(elementType.getClassName()), elementType.getDimensions()).getClass();
			case Type.OBJECT:
				return Class.forName(type.getClassName());
			default:
				throw new IllegalArgumentException("Unknown type: " + type);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isArray(Type type) {
		return type.getSort() == ARRAY;
	}

}
