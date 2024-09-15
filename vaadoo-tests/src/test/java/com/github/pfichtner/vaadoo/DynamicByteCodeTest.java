package com.github.pfichtner.vaadoo;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Stream.concat;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.PersistenceHandler;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.WithNull;

/**
 * This is the base of all dynamic tests (we can check all the different types
 * easily)
 */
class DynamicByteCodeTest {

	static record Config(List<ConfigEntry> entries) {
		private static Config config() {
			return new Config(Collections.emptyList());
		}

		public <T> Config withEntry(Class<T> paramType, String name, T value, Class<? extends Annotation> annoClass) {
			var newEntry = new ConfigEntry(paramType, name, value, annoClass);
			return new Config(concat(entries.stream(), Stream.of(newEntry)).toList());
		}
	}

	static record ConfigEntry(Class<?> paramType, String name, Object value, Class<? extends Annotation> annoClass) {
	}

	AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin();

	@Test
	void testGeneratedClassWithConstructor() throws Exception {
		var config = Config.config() //
				.withEntry(String.class, "parameter1", " ", NotNull.class) //
				.withEntry(String.class, "parameter2", " ", NotBlank.class);
		// TODO add the moment the ByteBuddy generated code contains debug information,
		// we want to support (and test both): Bytecode with and w/o debug information
		var transformedClass = transform(dynamicClass(config));
		assertThat(expectException(transformedClass, config)) //
				.withFailMessage("expected to throw exception but didn't") //
				.hasValueSatisfying(e -> assertThat(e).hasMessageContaining("parameter2 must not be blank"));
	}

	@Property
	void notBlankOks(@ForAll("charSequenceClass") Class<?> clazz, @ForAll("nonblanks") String nonBlankString)
			throws Exception {
		String parameterName = "parameter";
		var config = Config.config() //
				.withEntry(casted(clazz, CharSequence.class), parameterName, nonBlankString, NotBlank.class);
		var transformedClass = transform(dynamicClass(config));
		assertThat(expectException(transformedClass, config)).isEmpty();
	}

	@Property
	void notBlankNoks(@ForAll("charSequenceClass") Class<?> clazz, @WithNull @ForAll("blanks") String blankString)
			throws Exception {
		String parameterName = "parameter";
		var config = Config.config() //
				.withEntry(casted(clazz, CharSequence.class), parameterName, blankString, NotBlank.class);
		var transformedClass = transform(dynamicClass(config));
		assertThat(expectException(transformedClass, config)) //
				.withFailMessage("expected to throw exception but didn't") //
				.hasValueSatisfying(e -> {
					assertThat(e).hasMessageContaining(
							parameterName + " must not be " + (blankString == null ? "null" : "blank"));
				});
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> casted(Class<?> clazzArg, Class<T> target) {
		return (Class<T>) clazzArg;
	}

	@Provide("charSequenceClass")
	Arbitrary<Class<? extends CharSequence>> charSeqenceClasses() {
		return Arbitraries.of(CharSequence.class, String.class);
	}

	@Provide("blanks")
	Arbitrary<String> blanks() {
		return Arbitraries.of("", " ", "     ");
	}

	@Provide("nonblanks")
	Arbitrary<String> nonblanks() {
		return Arbitraries.of("x", "xXx", "x ", " x");
	}

	private static Optional<Throwable> expectException(Class<?> dynamicClass, Config config)
			throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException {
		var constructor = dynamicClass.getDeclaredConstructor(types(config.entries));
		try {
			constructor.newInstance(params(config.entries));
			return Optional.empty();
		} catch (InvocationTargetException e) {
			return Optional.of(e.getCause());
		}
	}

	private static Class<?>[] types(List<ConfigEntry> values) {
		return values.stream().map(ConfigEntry::paramType).toArray(Class[]::new);
	}

	private static Object[] params(List<ConfigEntry> values) {
		return values.stream().map(ConfigEntry::value).toArray();
	}

	private Unloaded<Object> dynamicClass(Config config) throws NoSuchMethodException {
		return dynamicClass("com.example.GeneratedTestClass", config.entries);
	}

	private static Unloaded<Object> dynamicClass(String name, List<ConfigEntry> values) throws NoSuchMethodException {
		var builder = new ByteBuddy().subclass(Object.class).name(name).defineConstructor(PUBLIC);

		Annotatable<Object> inner = null;
		for (ConfigEntry value : values) {
			inner = inner == null ? builder.withParameter(value.paramType, value.name())
					: inner.withParameter(value.paramType, value.name());
			inner = value.annoClass == null //
					? inner //
					: inner.annotateParameter(AnnotationDescription.Builder.ofType(value.annoClass).build());
		}

		var builderSuf = inner == null ? builder : inner;
		return builderSuf.intercept(MethodCall.invoke(Object.class.getConstructor())).make();
	}

	private Class<?> transform(Unloaded<Object> dynamicClass) throws NoSuchMethodException, ClassNotFoundException {
		var name = dynamicClass.getTypeDescription().getName();
		var originalClassLoader = sut.getClass().getClassLoader();

		var loadedClass = new ByteArrayClassLoader(originalClassLoader, singletonMap(name, dynamicClass.getBytes()),
				PersistenceHandler.MANIFEST).loadClass(name);
		var builder = new ByteBuddy().redefine(loadedClass);
		var transformed = sut.apply(builder, TypeDescription.ForLoadedType.of(loadedClass), null).make();
		var transformedClassLoader = new ByteArrayClassLoader(originalClassLoader, emptyMap(),
				PersistenceHandler.MANIFEST);
		return transformed.load(transformedClassLoader, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
	}

}
