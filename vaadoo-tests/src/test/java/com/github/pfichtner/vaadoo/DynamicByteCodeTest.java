package com.github.pfichtner.vaadoo;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Stream.concat;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.pfichtner.vaadoo.supplier.Blanks;
import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Classes.Types;
import com.github.pfichtner.vaadoo.supplier.NonBlanks;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.PersistenceHandler;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Tuple.Tuple2;
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

	// TODO add the moment the ByteBuddy generated code contains debug information,
	// we want to support (and test both): Bytecode with and w/o debug information
	AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin();

	@Property
	void showcaseWithThreeParams( //
			@ForAll(supplier = Classes.class) @Types(CharSequence.class) Tuple2<Class<Object>, Object> tuple1, //
			@ForAll(supplier = Classes.class) @Types(CharSequence.class) Tuple2<Class<Object>, Object> tuple2, //
			@ForAll(supplier = Classes.class) @Types(CharSequence.class) Tuple2<Class<Object>, Object> tuple3, //
			@ForAll(supplier = Blanks.class) String blank //
	) throws Exception {
		var config = Config.config() //
				.withEntry(casted(tuple1.get1(), CharSequence.class), "parameter1", blank, NotNull.class) //
				.withEntry(casted(tuple2.get1(), CharSequence.class), "parameter2", blank, NotBlank.class) //
				.withEntry(casted(tuple3.get1(), CharSequence.class), "parameter3", blank, NotBlank.class);
		var transformedClass = transform(dynamicClass(config));
		assertException(config, transformedClass, //
				"parameter2 must not be blank", IllegalArgumentException.class);
	}

	@Property
	void nullOks(@ForAll(supplier = Classes.class) Tuple2<Class<Object>, Object> tuple) throws Exception {
		Object nullValue = null;
		var config = Config.config().withEntry(tuple.get1(), "param", nullValue, Null.class);
		var transformedClass = transform(dynamicClass(config));
		assertNoException(config, transformedClass);
	}

	@Property
	void nullNoks(@ForAll(supplier = Classes.class) Tuple2<Class<Object>, Object> tuple) throws Exception {
		var parameterName = "param";
		var config = Config.config().withEntry(tuple.get1(), parameterName, tuple.get2(), Null.class);
		var transformedClass = transform(dynamicClass(config));
		assertException(config, transformedClass, parameterName + " expected to be null",
				IllegalArgumentException.class);
	}

	@Property
	void notnullOks(@ForAll(supplier = Classes.class) Tuple2<Class<Object>, Object> tuple) throws Exception {
		var parameterName = "param";
		var config = Config.config().withEntry(tuple.get1(), parameterName, null, NotNull.class);
		var transformedClass = transform(dynamicClass(config));
		assertException(config, transformedClass, parameterName + " must not be null", NullPointerException.class);
	}

	@Property
	void notnullNoks(@ForAll(supplier = Classes.class) Tuple2<Class<Object>, Object> tuple) throws Exception {
		var config = Config.config().withEntry(tuple.get1(), "param", tuple.get2(), NotNull.class);
		var transformedClass = transform(dynamicClass(config));
		assertNoException(config, transformedClass);
	}

	@ParameterizedTest
	@MethodSource("booleanOkConfigs")
	void booleansOks(Config config) throws Exception {
		var transformedClass = transform(dynamicClass(config));
		assertNoException(config, transformedClass);
	}

	@ParameterizedTest
	@MethodSource("booleanNokConfigs")
	void booleansNoks(Config config) throws Exception {
		var transformedClass = transform(dynamicClass(config));
		var entry = config.entries().get(0);
		assertException(config, transformedClass,
				entry.name() + " should be " + ((boolean) entry.value() ? "false" : "true"),
				IllegalArgumentException.class);
	}

	static List<Config> booleanOkConfigs() {
		return List.of( //
				Config.config().withEntry(Boolean.class, "param", true, AssertTrue.class), //
				Config.config().withEntry(boolean.class, "param", true, AssertTrue.class), //
				Config.config().withEntry(Boolean.class, "param", false, AssertFalse.class), //
				Config.config().withEntry(boolean.class, "param", false, AssertFalse.class));
	}

	static List<Config> booleanNokConfigs() {
		return List.of( //
				Config.config().withEntry(Boolean.class, "param", false, AssertTrue.class), //
				Config.config().withEntry(boolean.class, "param", false, AssertTrue.class), //
				Config.config().withEntry(Boolean.class, "param", true, AssertFalse.class), //
				Config.config().withEntry(boolean.class, "param", true, AssertFalse.class));
	}

	@Property
	void notBlankOks( //
			@ForAll(supplier = Classes.class) @Types(CharSequence.class) Tuple2<Class<Object>, Object> tuple, //
			@ForAll(supplier = NonBlanks.class) String nonBlank //
	) throws Exception {
		var config = Config.config().withEntry(tuple.get1(), "param", nonBlank, NotBlank.class);
		var transformedClass = transform(dynamicClass(config));
		assertNoException(config, transformedClass);
	}

	@Property
	void notBlankNoks( //
			@ForAll(supplier = Classes.class) @Types(CharSequence.class) Tuple2<Class<Object>, Object> tuple, //
			@WithNull @ForAll(supplier = Blanks.class) String blankString //
	) throws Exception {
		var parameterName = "param";
		boolean stringIsNull = blankString == null;
		var config = Config.config() //
				.withEntry(casted(tuple.get1(), CharSequence.class), parameterName, blankString, NotBlank.class);
		var transformedClass = transform(dynamicClass(config));
		assertException(config, transformedClass, //
				parameterName + " must not be " + (stringIsNull ? "null" : "blank"),
				stringIsNull ? NullPointerException.class : IllegalArgumentException.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> casted(Class<?> clazzArg, Class<T> target) {
		return (Class<T>) clazzArg;
	}

	private static void assertException(Config config, Class<?> transformedClass, String description,
			Class<? extends Exception> type) throws Exception {
		assertThat(provideExecException(transformedClass, config)) //
				.withFailMessage("expected to throw exception but didn't") //
				.hasValueSatisfying(e -> assertThat(e).isExactlyInstanceOf(type).hasMessageContaining(description));
	}

	private void assertNoException(Config config, Class<?> transformedClass) throws Exception {
		assertThat(provideExecException(transformedClass, config)).isEmpty();
	}

	private static Optional<Throwable> provideExecException(Class<?> dynamicClass, Config config) throws Exception {
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
