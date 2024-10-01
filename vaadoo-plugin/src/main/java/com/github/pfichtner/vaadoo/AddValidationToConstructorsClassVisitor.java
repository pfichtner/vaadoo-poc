package com.github.pfichtner.vaadoo;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static net.bytebuddy.jar.asm.Opcodes.ACC_STATIC;
import static net.bytebuddy.jar.asm.Opcodes.ALOAD;
import static net.bytebuddy.jar.asm.Opcodes.ASM9;
import static net.bytebuddy.jar.asm.Opcodes.ILOAD;
import static net.bytebuddy.jar.asm.Opcodes.INVOKESTATIC;
import static net.bytebuddy.jar.asm.Opcodes.RETURN;
import static net.bytebuddy.jar.asm.Type.getType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.pfichtner.vaadoo.ParameterInfo.EnumEntry;
import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;

public class AddValidationToConstructorsClassVisitor extends ClassVisitor {

	private static class ConfigEntry {

		private final Class<? extends Annotation> anno;
		private final Type type;

		public ConfigEntry(Class<? extends Annotation> anno) {
			this.anno = anno;
			this.type = getType(anno);
		}

		Class<?> anno() {
			return anno;
		}

		Type type() {
			return type;
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

	// possible checks during compile time:
	// errors
	// - @Pattern: Is the pattern valid (compile it)
	// - @Size: Is min >= 0
	// - @Min: Is there a @Max that is < @Min's value
	// - @Max: Is there a @Min that is < @Max's value
	// - @NotNull: Is there also @Null
	// - @Null: Is there also @NotNull
	// warnings
	// - @NotNull: Annotations that checks for null as well like @NotBlank @NotEmpty
	// - @Null: most (all?) other annotations doesn't make sense
	private static final List<ConfigEntry> configs = List.of( //
			new FixedClassConfigEntry(Null.class, Object.class), //
			new FixedClassConfigEntry(NotNull.class, Object.class), //
			new FixedClassConfigEntry(NotBlank.class, CharSequence.class), //
			new ConfigEntry(NotEmpty.class) {
				@Override
				Class<?> resolveSuperType(Class<?> actual) {
					var validTypes = List.of(CharSequence.class, Collection.class, Map.class, Object[].class);
					return superType(actual, validTypes).orElseThrow(() -> {
						return annotationOnTypeNotValid(anno(), actual,
								validTypes.stream().map(Class::getName).collect(toList()));
					});
				}
			}, //
			new ConfigEntry(Size.class) {
				@Override
				Class<?> resolveSuperType(Class<?> actual) {
					var validTypes = List.of(CharSequence.class, Collection.class, Map.class, Object[].class);
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
			new ConfigEntry(Digits.class), //
			new ConfigEntry(Positive.class), //
			new ConfigEntry(PositiveOrZero.class), //
			new ConfigEntry(Negative.class), //
			new ConfigEntry(NegativeOrZero.class), //
			new ConfigEntry(DecimalMin.class), //
			new ConfigEntry(DecimalMax.class), //
			//
			new ConfigEntry(Future.class) {
				Class<?> resolveSuperType(Class<?> actual) {
					throw new IllegalStateException(anno() + " not yet supported");
				}
			}, //
			new ConfigEntry(FutureOrPresent.class) {
				Class<?> resolveSuperType(Class<?> actual) {
					throw new IllegalStateException(anno() + " not yet supported");
				}
			}, //
			new ConfigEntry(Past.class) {
				Class<?> resolveSuperType(Class<?> actual) {
					throw new IllegalStateException(anno() + " not yet supported");
				}
			}, //
			new ConfigEntry(PastOrPresent.class) {
				Class<?> resolveSuperType(Class<?> actual) {
					throw new IllegalStateException(anno() + " not yet supported");
				}
			} //
	);

	private static Optional<Class<?>> superType(Class<?> classToCheck, List<Class<?>> superTypes) {
		return superTypes.stream().filter(t -> t.isAssignableFrom(classToCheck)).findFirst();
	}

	private static IllegalStateException annotationOnTypeNotValid(Class<?> anno, Class<?> type, List<String> valids) {
		return new IllegalStateException(format("Annotation %s on type %s not allowed, allowed only on types: %s",
				anno.getName(), type.getName(), valids));
	}

	private static class ConstructorVisitor extends MethodVisitor {

		private final String className;
		private final ClassMembers classMembers;
		private Parameters parameters;
		private String methodAddedName;
		private boolean visitParameterCalled;
		private boolean validationAdded;

		public ConstructorVisitor(MethodVisitor methodVisitor, String className, String methodDescriptor,
				ClassMembers classMembers) {
			super(ASM9, methodVisitor);
			this.className = className;
			this.parameters = Parameters.fromDescriptor(methodDescriptor);
			this.classMembers = classMembers;
		}

		@Override
		public void visitParameter(String name, int access) {
			visitParameterCalled = true;
			parameters.firstUnamed().name(name);
			super.visitParameter(name, access);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			var parameterInfo = parameters.byIndex(parameter);
			parameterInfo.addAnnotation(getType(descriptor));
			return new AnnotationVisitor(ASM9) {

				private Type annotationType = getType(descriptor);

				@Override
				public void visit(String name, Object value) {
					parameterInfo.addAnnotationValue(annotationType, name, value);
					super.visit(name, value);
				}

				@Override
				public AnnotationVisitor visitArray(String arrayName) {
					return new AnnotationVisitor(ASM9) {

						private final List<EnumEntry> values = new ArrayList<>();

						@Override
						public void visitEnum(String name, String descriptor, String value) {
							values.add(new EnumEntry(getType(descriptor), value));
							super.visitEnum(name, descriptor, value);
						}

						@Override
						public void visitEnd() {
							parameterInfo.addAnnotationValue(annotationType, arrayName, values);
							super.visitEnd();
						}
					};
				}

			};
		}

		@Override
		public void visitVarInsn(int opcode, int varIndex) {
			super.visitVarInsn(opcode, varIndex);
			// This is not valid in source- but in bytecode (we call validate before the
			// super call)
			if (!validationAdded && opcode == ALOAD && varIndex == 0) {
				addValidateMethodCall(parameters.argumentTypes);
				validationAdded = true;
			}
		}

		private void addValidateMethodCall(Type[] methodAddedArgumentTypes) {
			if (methodAddedArgumentTypes.length > 0) {
				int index = 1;
				for (Type argType : methodAddedArgumentTypes) {
					mv.visitVarInsn(argType.getOpcode(ILOAD), index);
					index += argType.getSize();
				}
				methodAddedName = classMembers.newMethod("validate");
				mv.visitMethodInsn(INVOKESTATIC, className, methodAddedName,
						Type.getMethodDescriptor(Type.VOID_TYPE, methodAddedArgumentTypes), false);
			}
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
				int index) {
			int offset = index - 1; // 0 is this on non-statics
			if (!visitParameterCalled && offset >= 0) {
				parameters.byOffset(offset).name(name);
			}
			super.visitLocalVariable(name, descriptor, signature, start, end, index);
		}

	}

	private String className;
	private final List<ConstructorVisitor> constructorVisitors = new ArrayList<>();
	private final ClassMembers classMembers;
	private Class<? extends Jsr380CodeFragment> codeFragment;
	private List<Method> codeFragmentMethods;

	AddValidationToConstructorsClassVisitor(ClassVisitor outputVisitor,
			Class<? extends Jsr380CodeFragment> codeFragment, List<Method> codeFragmentMethods,
			ClassMembers classMembers) {
		super(ASM9, outputVisitor);
		this.codeFragment = codeFragment;
		this.codeFragmentMethods = codeFragmentMethods;
		this.classMembers = classMembers;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		if (!"<init>".equals(name)) {
			return mv;
		}

		ConstructorVisitor constructorVisitor = new ConstructorVisitor(mv, className, descriptor, classMembers);
		constructorVisitors.add(constructorVisitor);
		return constructorVisitor;
	}

	@Override
	public void visitEnd() {
		for (var visitor : constructorVisitors) {
			if (visitor.methodAddedName != null) {
				addValidateMethod(visitor.methodAddedName, visitor.parameters);
			}
		}
		super.visitEnd();
	}

	private void addValidateMethod(String name, Parameters parameters) {
		var signature = parameters.methodDescriptor;
		MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC, name, signature, null, null);
		mv.visitCode();

		var injector = new MethodInjector(codeFragment, signature);
		for (var parameter : parameters) {
			for (var annotation : parameter.getAnnotations()) {
				for (var config : configs) {
					if (annotation.equals(config.type())) {
						injector.inject(mv, parameter, checkMethod(config, parameter.classtype()));
					}
				}
			}
		}

		mv.visitInsn(RETURN);
		mv.visitMaxs(parameters.size(), parameters.size());
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
		var supported = this.codeFragmentMethods.stream() //
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

	private boolean isCheckMethod(Method method) {
		return "check".equals(method.getName());
	}
}