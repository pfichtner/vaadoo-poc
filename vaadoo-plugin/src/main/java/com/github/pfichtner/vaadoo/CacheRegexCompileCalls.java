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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.pfichtner.vaadoo.fragments.impl.RegexPatternCache;
import com.github.pfichtner.vaadoo.fragments.impl.RegexWithFlagsPatternCache;

import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;

public class CacheRegexCompileCalls extends ClassVisitor {

	private static class Fragment {

		private final Class<?> fragment;
		private final String onlyMethodsDescriptor;

		public Fragment(Class<?> fragment) {
			this.fragment = fragment;
			this.onlyMethodsDescriptor = getMethodDescriptor(onlyMethod(fragment));
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

	private static final String CACHED_REGEX_METHODNAME = "cachedRegex";

	private String classname;
	private final Set<String> argTypeOfReplacedCallsToPatternCompile = new HashSet<>();

	public CacheRegexCompileCalls(int api, ClassVisitor targetClassVisitor) {
		super(api, targetClassVisitor);
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
					argTypeOfReplacedCallsToPatternCompile.add(descriptor);
					// TODO again: make method name unique
					super.visitMethodInsn(INVOKESTATIC, classname, CACHED_REGEX_METHODNAME, descriptor, false);
				} else {
					super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
				}
			}

		};
	}

	@Override
	public void visitEnd() {
		for (Fragment fragment : fragements) {
			if (argTypeOfReplacedCallsToPatternCompile.contains(fragment.onlyMethodsDescriptor)) {
				classReader(fragment.fragment).accept(copyFieldsAndMethods(fragment.fragment), 0);
			}
		}
		super.visitEnd();
	}

	private ClassVisitor copyFieldsAndMethods(Class<?> clazz) {
		return new ClassVisitor(ASM9) {

			private final String fragmentClassName = Type.getType(clazz).getInternalName();

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				return CacheRegexCompileCalls.this.cv.visitField(access, name, descriptor, signature, value);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				if (!"<clinit>".equals(name) && !name.equals(CACHED_REGEX_METHODNAME)
						&& (ACC_SYNTHETIC & access) == 0) {
					return null;
				}

				return new MethodVisitor(ASM9,
						CacheRegexCompileCalls.this.cv.visitMethod(access, name, descriptor, signature, exceptions)) {
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						// TODO prevent clashes on field names --> Don't forget to change the field
						// accesses as well!
						super.visitFieldInsn(opcode, classname, name, descriptor);
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