package com.github.pfichtner.vaadoo;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.PersistenceHandler;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;

/**
 * This is the base of all dynamic tests (we can check all the different types
 * easily)
 */
class DynamicByteCodeTest {

	private static record Config(Class<?> paramType, String name, Object value, Class<? extends Annotation> annoClass) {
	}

	@Test
	void testGeneratedClassWithConstructor() throws Exception {
		var configs = List.of( //
				new Config(String.class, "parameter1", " ", NotNull.class), //
				new Config(String.class, "parameter2", " ", NotBlank.class)//
		);
		// TODO add the moment the ByteBuddy generated code contains debug information,
		// we want to support (and test both): Bytecode with and w/o debug information
		var transformedClass = transform(new AddJsr380ValidationPlugin(),
				dynamicClass("com.example.GeneratedTestClass", configs));
		assertThat(expectException(transformedClass, configs)) //
				.withFailMessage("expected to throw exception but didn't") //
				.hasValueSatisfying(e -> assertThat(e).hasMessageContaining("parameter2 must not be blank"));
	}

	private static Optional<Throwable> expectException(Class<?> dynamicClass, List<Config> configs)
			throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException {
		var constructor = dynamicClass.getDeclaredConstructor(types(configs));
		try {
			constructor.newInstance(params(configs));
			return Optional.empty();
		} catch (InvocationTargetException e) {
			return Optional.of(e.getCause());
		}
	}

	private static Class<?>[] types(List<Config> values) {
		return values.stream().map(Config::paramType).toArray(Class[]::new);
	}

	private static Object[] params(List<Config> values) {
		return values.stream().map(Config::value).toArray();
	}

	private static Unloaded<Object> dynamicClass(String name, List<Config> values) throws NoSuchMethodException {
		var builder = new ByteBuddy().subclass(Object.class).name(name).defineConstructor(PUBLIC);

		Annotatable<Object> inner = null;
		for (Config value : values) {
			inner = inner == null ? builder.withParameter(value.paramType, value.name())
					: inner.withParameter(value.paramType, value.name());
			inner = value.annoClass == null //
					? inner //
					: inner.annotateParameter(AnnotationDescription.Builder.ofType(value.annoClass).build());
		}

		var builderSuf = inner == null ? builder : inner;
		return builderSuf.intercept(MethodCall.invoke(Object.class.getConstructor())).make();
	}

	private static Class<?> transform(Plugin plugin, Unloaded<Object> dynamicClass)
			throws NoSuchMethodException, ClassNotFoundException {
		var name = dynamicClass.getTypeDescription().getName();
		var originalClassLoader = plugin.getClass().getClassLoader();

		var loadedClass = new ByteArrayClassLoader(originalClassLoader, singletonMap(name, dynamicClass.getBytes()),
				PersistenceHandler.MANIFEST).loadClass(name);
		var builder = new ByteBuddy().redefine(loadedClass);
		var transformed = plugin.apply(builder, TypeDescription.ForLoadedType.of(loadedClass), null).make();
		var transformedClassLoader = new ByteArrayClassLoader(originalClassLoader, emptyMap(),
				PersistenceHandler.MANIFEST);
		return transformed.load(transformedClassLoader, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
	}

}
