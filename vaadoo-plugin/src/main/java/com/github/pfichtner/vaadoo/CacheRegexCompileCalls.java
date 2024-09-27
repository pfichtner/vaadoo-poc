package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.AsmUtil.classReader;
import static java.util.Arrays.stream;
import static java.util.function.Predicate.not;
import static net.bytebuddy.jar.asm.Opcodes.ACC_SYNTHETIC;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Opcodes.H_INVOKESTATIC;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;
import static net.bytebuddy.jar.asm.Type.getMethodDescriptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.pfichtner.vaadoo.fragments.impl.RegexPatternCache;
import com.github.pfichtner.vaadoo.fragments.impl.RegexWithFlagsPatternCache;

import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;

public class CacheRegexCompileCalls extends ClassVisitor {

	private static final String METHOD_NAME_IN_FRAGMENT = "cache";

	private static class Fragment {

		private final Class<?> clazz;
		private final String onlyMethodsDescriptor;

		public Fragment(Class<?> fragmentClass) {
			this.clazz = fragmentClass;
			this.onlyMethodsDescriptor = getMethodDescriptor(checkMethodName(onlyMethod(fragmentClass)));
		}

		private Method checkMethodName(Method method) {
			if (!METHOD_NAME_IN_FRAGMENT.equals(method.getName())) {
				throw new IllegalStateException("Currently the name of the method in the fragment has to be "
						+ METHOD_NAME_IN_FRAGMENT + " but was " + method.getName());
			}
			return method;
		}

		private static Method onlyMethod(Class<?> clazz) {
			return stream(clazz.getDeclaredMethods()).filter(not(Method::isSynthetic)).reduce((m0, m1) -> {
				throw new IllegalStateException("Expected to find exactly one method in " + clazz.getName());
			}).orElseThrow(
					() -> new IllegalStateException("Expected to find exactly one method in " + clazz.getName()));
		}
	}

	private static final List<Fragment> fragements = List.of( //
			new Fragment(RegexPatternCache.class), //
			new Fragment(RegexWithFlagsPatternCache.class) //
	);

	private final ClassMembers classMembers;
	private String classname;
	private final Map<String, String> cachedRegexMethodnames = new HashMap<>();

	public CacheRegexCompileCalls(ClassVisitor targetClassVisitor, ClassMembers classMembers) {
		super(ASM9, targetClassVisitor);
		this.classMembers = classMembers;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classname = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
				if (opcode == INVOKESTATIC && "java/util/regex/Pattern".equals(owner) && "compile".equals(name)) {
					super.visitMethodInsn(INVOKESTATIC, classname, cachedRegexMethodnames.computeIfAbsent(descriptor,
							d -> classMembers.newMethod(METHOD_NAME_IN_FRAGMENT)), descriptor, false);
				} else {
					super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
				}
			}

		};
	}

	@Override
	public void visitEnd() {
		for (Fragment fragment : fragements) {
			String methodNameUsed = cachedRegexMethodnames.get(fragment.onlyMethodsDescriptor);
			if (methodNameUsed != null) {
				classReader(fragment.clazz).accept(copyFieldsAndMethods(fragment.clazz, methodNameUsed), 0);
			}
		}
		super.visitEnd();
	}

	private ClassVisitor copyFieldsAndMethods(Class<?> clazz, String methodCalled) {
		return new ClassVisitor(ASM9) {

			private final String fragmentClassName = Type.getType(clazz).getInternalName();
			private final Map<String, String> fieldNames = new HashMap<>();

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				String fieldName = classMembers.containsFieldName(name) ? classMembers.newField(name) : name;
				this.fieldNames.put(name, fieldName);
				return CacheRegexCompileCalls.this.cv.visitField(access, fieldName, descriptor, signature, value);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {

				// TODO this fails if we add <clinit> but there is already a <clinit> present
				// TODO Also the synthetic methods have to get renamed if they already exist
				boolean isFragmentMethod = name.equals(METHOD_NAME_IN_FRAGMENT);
				if (!"<clinit>".equals(name) && !isFragmentMethod && (ACC_SYNTHETIC & access) == 0) {
					return null;
				}

				if (isFragmentMethod) {
					// name could differ (we tried to add method "cache" to the class but because
					// "cache" already was present the name got "cache$1" (we first insert the
					// calls, then we add the method, which is done here)
					name = methodCalled;
				}

				return new MethodVisitor(ASM9,
						CacheRegexCompileCalls.this.cv.visitMethod(access, name, descriptor, signature, exceptions)) {

					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						super.visitFieldInsn(opcode, classname, fieldNames.get(name), descriptor);
					}

					@Override
					public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle,
							Object... bootstrapMethodArguments) {
						if (H_INVOKESTATIC == handle.getTag()
								&& "java/lang/invoke/LambdaMetafactory".equals(handle.getOwner())) {
							for (int i = 0; i < bootstrapMethodArguments.length; i++) {
								if (bootstrapMethodArguments[i] instanceof Handle) {
									Handle innerHandle = (Handle) bootstrapMethodArguments[i];
									if (H_INVOKESTATIC == innerHandle.getTag()
											&& fragmentClassName.equals(innerHandle.getOwner())) {
										bootstrapMethodArguments[i] = new Handle(innerHandle.getTag(), classname,
												innerHandle.getName(), innerHandle.getDesc(),
												innerHandle.isInterface());
									}
								}
							}
						}
						super.visitInvokeDynamicInsn(name, descriptor, handle, bootstrapMethodArguments);
					}

				};
			}

		};
	}

}