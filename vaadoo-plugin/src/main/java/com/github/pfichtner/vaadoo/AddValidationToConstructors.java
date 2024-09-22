package com.github.pfichtner.vaadoo;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static net.bytebuddy.jar.asm.ClassWriter.COMPUTE_FRAMES;
import static net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static net.bytebuddy.jar.asm.Opcodes.ACC_STATIC;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Opcodes.ILOAD;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESPECIAL;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;
import static net.bytebuddy.jar.asm.Opcodes.RETURN;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.pool.TypePool;

public class AddValidationToConstructors implements AsmVisitorWrapper {

	private final Class<? extends Jsr380CodeFragment> codeFragment;
	private final List<Method> codeFragmentMethods;

	public AddValidationToConstructors(Class<? extends Jsr380CodeFragment> codeFragment) {
		this.codeFragment = codeFragment;
		this.codeFragmentMethods = Arrays.asList(codeFragment.getMethods());
	}

	private static class ConfigEntry {
		private Class<? extends Annotation> anno;

		public ConfigEntry(Class<? extends Annotation> anno) {
			this.anno = anno;
		}

		Class<?> anno() {
			return anno;
		}

		String descriptor() {
			return Type.getDescriptor(anno());
		}

		Class<?> resolveSuperType(Class<?> actual) {
			return actual;
		}
	}

	private static class FixedClassConfigEntry extends ConfigEntry {

		private Class<?> superType;

		public FixedClassConfigEntry(Class<? extends Annotation> anno, Class<?> superType) {
			super(anno);
			this.superType = superType;
		}

		@Override
		public Class<?> resolveSuperType(Class<?> actual) {
			return superType;
		}

	}

	private static final List<ConfigEntry> entries = List.of( //
			new FixedClassConfigEntry(Null.class, Object.class), //
			new FixedClassConfigEntry(NotNull.class, Object.class), //
			new FixedClassConfigEntry(NotBlank.class, CharSequence.class), //
			new ConfigEntry(NotEmpty.class) {
				@Override
				Class<?> resolveSuperType(Class<?> actual) {
					var validTypes = List.of(Object[].class, CharSequence.class, Collection.class, Map.class);
					return superType(actual, validTypes).orElseThrow(() -> {
						return annotationOnTypeNotValid(anno(), actual,
								validTypes.stream().map(Class::getName).collect(toList()));
					});
				}
			}, //
			new FixedClassConfigEntry(Pattern.class, CharSequence.class), //
			new FixedClassConfigEntry(Email.class, CharSequence.class), //
			new ConfigEntry(AssertTrue.class), //
			new ConfigEntry(AssertFalse.class), //
			new ConfigEntry(Min.class), //
			new ConfigEntry(Max.class), //
			new ConfigEntry(Positive.class), //
			new ConfigEntry(Negative.class) //
	);

	private static Optional<Class<?>> superType(Class<?> classToCheck, List<Class<?>> superTypes) {
		return superTypes.stream().filter(t -> t.isAssignableFrom(classToCheck)).findFirst();
	}

	private static IllegalStateException annotationOnTypeNotValid(Class<?> anno, Class<?> type, List<String> valids) {
		return new IllegalStateException(format("Annotation %s on type %s not allowed, allowed only on types: %s",
				anno.getName(), type.getName(), valids));
	}

	@Override
	public int mergeReader(int flags) {
		return flags;
	}

	@Override
	public int mergeWriter(int flags) {
		return flags | COMPUTE_FRAMES;
	}

	@Override
	public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Context implementationContext,
			TypePool typePool, FieldList<InDefinedShape> fields, MethodList<?> methods, int writerFlags,
			int readerFlags) {
		return new ClassVisitor(ASM9, classVisitor) {

			private String className;
			private final List<ConstructorVisitor> constructorVisitors = new ArrayList<>();
			private final List<String> methodNames = new ArrayList<>();

			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				this.className = name;
				super.visit(version, access, name, signature, superName, interfaces);
				methods.stream().map(MethodDescription::getName).forEach(methodNames::add);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (!"<init>".equals(name)) {
					return mv;
				}

				ConstructorVisitor constructorVisitor = new ConstructorVisitor(ASM9, mv, className, descriptor,
						methodNames);
				constructorVisitors.add(constructorVisitor);
				return constructorVisitor;
			}

			@Override
			public void visitEnd() {
				super.visitEnd();
				for (ConstructorVisitor visitor : constructorVisitors) {
					if (visitor.methodAddedName != null) {
						addValidateMethod(visitor.methodAddedName, visitor.methodDescriptor, visitor.parameterInfos);
					}
				}
			}

			private void addValidateMethod(String name, String signature, Map<Integer, ParameterInfo> parameters) {
				MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC, name, signature, null, null);
				mv.visitCode();

				var injector = new MethodInjector(codeFragment, signature);
				for (var parameter : parameters.values()) {
					for (var annotation : parameter.getAnnotations()) {
						for (var config : entries) {
							if (annotation.equals(config.descriptor())) {
								injector.inject(mv, parameter, checkMethod(config, parameter.classtype()));
							}
						}
					}
				}

				mv.visitInsn(RETURN);
				mv.visitMaxs(parameters.entrySet().size(), parameters.entrySet().size());
				mv.visitEnd();
			}

			private Method checkMethod(ConfigEntry config, Class<?> actual) {
				Class<?>[] parameters = new Class[] { config.anno(), config.resolveSuperType(actual) };
				return checkMethod(parameters).map(m -> {
					var supportedType = m.getParameterTypes()[1];
					if (supportedType.isAssignableFrom(actual)) {
						return m;
					}
					throw annotationOnTypeNotValid(parameters[0], actual, List.of(supportedType.getName()));
				}).orElseThrow(() -> unsupportedType(parameters));
			}

			private IllegalStateException unsupportedType(Class<?>... parameters) {
				assert parameters.length >= 2 : "Expected to get 2 parameters, got " + Arrays.toString(parameters);
				var supported = codeFragmentMethods.stream() //
						.filter(this::isCheckMethod) //
						.filter(m -> m.getParameterCount() > 1) //
						.filter(m -> m.getParameterTypes()[0] == parameters[0]) //
						.map(m -> m.getParameterTypes()[1].getName()) //
						.collect(toList());
				return annotationOnTypeNotValid(parameters[0], parameters[1], supported);
			}

			private Optional<Method> checkMethod(Class<?>... parameters) {
				return codeFragmentMethods.stream().filter(this::isCheckMethod)
						.filter(m -> Arrays.equals(m.getParameterTypes(), parameters)).findFirst();
			}

			private boolean isCheckMethod(Method m) {
				return "check".equals(m.getName());
			}

		};
	}

	private static class ConstructorVisitor extends MethodVisitor {

		private final String className;
		private final String methodDescriptor;
		private final List<String> methodNames;
		private final Map<Integer, ParameterInfo> parameterInfos = new TreeMap<>();
		private String methodAddedName;
		private boolean visitParameterCalled;

		public ConstructorVisitor(int api, MethodVisitor methodVisitor, String className, String methodDescriptor,
				List<String> methodNames) {
			super(api, methodVisitor);
			this.className = className;
			this.methodDescriptor = methodDescriptor;
			this.methodNames = methodNames;
		}

		@Override
		public void visitParameter(String name, int access) {
			visitParameterCalled = true;
			int index = parameterInfos.size();
			parameterInfo(index).name(name).type(Type.getMethodType(methodDescriptor).getArgumentTypes()[index]);
			super.visitParameter(name, access);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			ParameterInfo parameterInfo = parameterInfo(parameter);
			parameterInfo.addAnnotation(descriptor);
			return new AnnotationVisitor(ASM9) {

				@Override
				public void visit(String name, Object value) {
					parameterInfo.addAnnotationValue(name, value);
					super.visit(name, value);
				}

				@Override
				public AnnotationVisitor visitArray(String arrayName) {
					return new AnnotationVisitor(ASM9) {
						@Override
						public void visitEnum(String name, String descriptor, String value) {
							parameterInfo.addAnnotationArrayElement(arrayName, Type.getType(descriptor), value);
							super.visitEnum(name, descriptor, value);
						}
					};
				}

			};
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			if (opcode == INVOKESPECIAL && name.equals("<init>")) {
				addValidateMethodCall(Type.getArgumentTypes(methodDescriptor));
			}
		}

		private void addValidateMethodCall(Type[] methodAddedArgumentTypes) {
			if (methodAddedArgumentTypes.length > 0) {
				int index = 1;
				for (Type argType : methodAddedArgumentTypes) {
					mv.visitVarInsn(argType.getOpcode(ILOAD), index);
					index += argType.getSize();
				}
				methodAddedName = constructMethodName();
				methodNames.add(methodAddedName);
				mv.visitMethodInsn(INVOKESTATIC, className, methodAddedName,
						Type.getMethodDescriptor(Type.VOID_TYPE, methodAddedArgumentTypes), false);
			}
		}

		private String constructMethodName() {
			String methodName = "validate";
			for (int i = 1; methodNames.contains(methodName); i++) {
				methodName = "validate$" + i;
			}
			return methodName;
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
				int index) {
			if (!visitParameterCalled) {
				int parameter = index - 1;
				if (parameter >= 0) {
					parameterInfo(parameter).name(name).type(Type.getType(descriptor));
				}
			}
			super.visitLocalVariable(name, descriptor, signature, start, end, index);
		}

		private ParameterInfo parameterInfo(int parameter) {
			return parameterInfos.computeIfAbsent(parameter, idx -> new ParameterInfo(idx.intValue()));
		}

	}

}
