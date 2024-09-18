package com.github.pfichtner.vaadoo;

import static net.bytebuddy.jar.asm.Opcodes.ASM9;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.github.pfichtner.vaadoo.fragments.CodeFragment;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

public class MethodInjector {

	private final Class<? extends CodeFragment> clazz;
	private final ClassReader classReader;

	public MethodInjector(Class<? extends CodeFragment> clazz) {
		this.clazz = clazz;
		String className = clazz.getName();
		try {
			ClassFileLocator classFileLocator = ClassFileLocator.ForClassLoader.ofSystemLoader();
			ClassFileLocator.Resolution resolution = classFileLocator.locate(className.replace('.', '/'));

			if (!resolution.isResolved()) {
				throw new IllegalStateException("Class " + className + " not found on classpath.");
			}

			this.classReader = new ClassReader(resolution.resolve());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void injectCode(MethodVisitor mv, ParameterInfo parameter, String methodName, Class<?>... parameters) {
		Method method = findMethod(methodName, parameters);
		// TODO fixme
//		boolean isStatic = (method.getModifiers() | Modifier.STATIC) != 0;
		boolean isStatic = false;

		this.classReader.accept(new ClassVisitor(ASM9) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {

				String searchDescriptor = Type.getMethodDescriptor(method);

				if (name.equals(methodName) && descriptor.equals(searchDescriptor)) {
					return new MethodVisitor(ASM9, mv) {

						private boolean firstParamLoadStart;

						@Override
						public void visitLineNumber(int line, Label start) {
							// ignore
						}

						@Override
						public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
							// ignore
						}

						@Override
						public void visitMaxs(int maxStack, int maxLocals) {
							// ignore
						}

						@Override
						public void visitVarInsn(int opcode, int var) {
							boolean isLoad = isLoadOpcode(opcode);
							if (isLoad && var == 1) {
								firstParamLoadStart = true;
							} else {
								super.visitVarInsn(opcode,
										isLoad ? var + parameter.index() - 1 - (isStatic ? 0 : 1) : var);
							}
						}

						public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
								boolean isInterface) {
							if (firstParamLoadStart) {
								Long value = parameter.annotationValue(name).map(String::valueOf).map(Long::valueOf)
										.orElseThrow(() -> new IllegalStateException(
												owner + " does not define attribute '" + name + "'"));
								visitLdcInsn(value);
								firstParamLoadStart = false;
							} else {
								super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
							}
						};

						@Override
						public void visitInsn(int opcode) {
							if (!isReturnOpcode(opcode)) {
								super.visitInsn(opcode);
							}
						}

						private boolean isLoadOpcode(int opcode) {
							return opcode == Opcodes.ALOAD //
									|| opcode == Opcodes.ILOAD //
									|| opcode == Opcodes.LLOAD //
									|| opcode == Opcodes.FLOAD //
									|| opcode == Opcodes.DLOAD;
						}

						private boolean isReturnOpcode(int opcode) {
							return opcode == Opcodes.ARETURN //
									|| opcode == Opcodes.IRETURN //
									|| opcode == Opcodes.LRETURN //
									|| opcode == Opcodes.FRETURN //
									|| opcode == Opcodes.DRETURN;
						}

						@Override
						public void visitLdcInsn(Object value) {
							super.visitLdcInsn(value instanceof String //
									? String.format((String) value, parameter.name()) //
									: value);
						}

						public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle,
								Object... args) {
							if ("makeConcatWithConstants".equals(name) && "(J)Ljava/lang/String;".equals(descriptor)
									&& "makeConcatWithConstants".equals(handle.getName())
									&& "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
											.equals(handle.getDesc())
									&& "java/lang/invoke/StringConcatFactory".equals(handle.getOwner())
									&& args.length >= 0 && args[0] instanceof String) {
								args[0] = String.format((String) args[0], parameter.name());
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
