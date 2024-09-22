package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.NumberWrapper.numberWrapper;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Supplier;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationDescription.Builder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader.PersistenceHandler;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;

public final class DynamicByteCode {

	private static final boolean DEBUG = false;

	// TODO add the moment the ByteBuddy generated code contains debug information,
	// we want to support (and test both): Bytecode with and w/o debug information
	static AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin();

	private DynamicByteCode() {
		super();
	}

	public static record Config(List<ConfigEntry> entries) {

		public Config(List<ConfigEntry> entries) {
			this.entries = List.copyOf(entries);
		}

		public static Config config() {
			return new Config(emptyList());
		}

		public Config withEntry(ConfigEntry newEntry) {
			return new Config(concat(entries.stream(), Stream.of(newEntry)).toList());
		}

	}

	public static record ConfigEntry(Class<?> paramType, String name, Object value,
			Class<? extends Annotation> annoClass, Map<String, Object> annoValues) {
		public static <T> ConfigEntry entry(Class<T> paramType, String name, T value) {
			return new ConfigEntry(paramType, name, value, null, emptyMap());
		}

		public ConfigEntry withAnno(Class<? extends Annotation> annoClass) {
			return withAnno(annoClass, emptyMap());
		}

		public ConfigEntry withAnno(Class<? extends Annotation> annoClass, Map<String, Object> annoValues) {
			return new ConfigEntry(paramType(), name(), value(), annoClass, annoValues);
		}
	}

	static boolean lowerBoundInLongRange(Number value) {
		return new BigDecimal(value.toString()).compareTo(new BigDecimal(Long.MIN_VALUE)) >= 0;
	}

	static boolean upperBoundInLongRange(Number value) {
		return new BigDecimal(value.toString()).compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0;
	}

	public static Config randomConfigWith(ConfigEntry entry) {
		SecureRandom random = new SecureRandom();
		// TODO use random classes
		Supplier<ConfigEntry> supplier = () -> entry(Object.class, "param" + randomUUID().toString().replace("-", "_"),
				randomUUID());
		return new Config(Stream.of( //
				Stream.generate(supplier).limit(random.nextInt(5)), //
				Stream.of(entry), //
				Stream.generate(supplier).limit(random.nextInt(5)) //
		).reduce(empty(), Stream::concat).toList());
	}

	@SuppressWarnings("unchecked")
	public static Object convertValue(Object in, boolean empty) {
		Object result = in;
		if (empty) {
			if (result instanceof CharSequence) {
				result = "";
			} else if (result instanceof Collection collection) {
				collection.clear();
			} else if (result instanceof Map map) {
				map.clear();
			} else if (result.getClass().isArray()) {
				result = Array.newInstance(result.getClass().getComponentType(), 0);
			}
		} else {
			if (result instanceof CharSequence) {
				result = " ";
			} else if (result instanceof Collection collection) {
				collection.add(new Object());
			} else if (result instanceof Map map) {
				map.put(new Object(), new Object());
			} else if (result.getClass().isArray()) {
				// empty but of size "one"
				result = Array.newInstance(result.getClass().getComponentType(), 1);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> casted(Class<?> clazzArg, Class<T> target) {
		return (Class<T>) clazzArg;
	}

	public static void assertException(Config config, Class<?> transformed, String description,
			Class<? extends Exception> type) throws Exception {
		assertException(provideExecException(transformed, config), description, type);
	}

	public static void assertException(Optional<Throwable> provideExecException, String description,
			Class<? extends Exception> type) {
		assertThat(provideExecException) //
				.withFailMessage("expected to throw exception but didn't") //
				.hasValueSatisfying(e -> assertThat(e).isExactlyInstanceOf(type).hasMessageContaining(description));
	}

	public static void assertNoException(Config config, Class<?> transformed) throws Exception {
		assertThat(provideExecException(transformed, config)).isEmpty();
	}

	public static void assertNoException(Optional<Throwable> execResult) {
		assertThat(execResult).isEmpty();
	}

	public static Optional<Throwable> provideExecException(Class<?> dynamicClass, Config config) throws Exception {
		var constructor = dynamicClass.getDeclaredConstructor(types(config.entries()));
		try {
			constructor.newInstance(params(config.entries()));
			return Optional.empty();
		} catch (InvocationTargetException e) {
			return Optional.of(e.getCause());
		}
	}

	private static Class<?>[] types(List<ConfigEntry> values) {
		return values.stream().map(ConfigEntry::paramType).toArray(Class[]::new);
	}

	private static Object[] params(List<ConfigEntry> values) {
		return values.stream().map(DynamicByteCode::castToTargetType).toArray();
	}

	private static Object castToTargetType(ConfigEntry entry) {
		return entry.paramType().isPrimitive() ? primCast(entry) : objCast(entry);
	}

	private static Object primCast(ConfigEntry entry) {
		return entry.paramType() == boolean.class //
				? (Boolean) entry.value() //
				: numberWrapper(entry.paramType(), entry.value()).value();
	}

	private static Object objCast(ConfigEntry entry) {
		return entry.paramType().cast(entry.value());
	}

	public static Unloaded<Object> dynamicClass(Config config) throws NoSuchMethodException {
		return dynamicClass("com.example.GeneratedTestClass", config.entries());
	}

	public static Unloaded<Object> dynamicClass(String name, List<ConfigEntry> values) throws NoSuchMethodException {
		var builder = new ByteBuddy().subclass(Object.class).name(name).defineConstructor(PUBLIC);

		Annotatable<Object> inner = null;
		for (ConfigEntry value : values) {
			inner = inner == null ? builder.withParameter(value.paramType(), value.name())
					: inner.withParameter(value.paramType(), value.name());
			if (value.annoClass() != null) {
				Builder annoBuilder = AnnotationDescription.Builder.ofType(value.annoClass());

				for (Entry<String, Object> entry : value.annoValues().entrySet()) {
					Object annoValue = entry.getValue();
					if (annoValue instanceof String) {
						annoBuilder = annoBuilder.define(entry.getKey(), (String) annoValue);
					} else if (annoValue instanceof Long) {
						annoBuilder = annoBuilder.define(entry.getKey(), (Long) annoValue);
					} else if (annoValue.getClass().isArray()) {
						Class<?> componentType = annoValue.getClass().getComponentType();
						if (componentType.isEnum()) {
							Object[] enumArray = (Object[]) annoValue;
							Enum<?>[] typedEnumArray = (Enum<?>[]) Array.newInstance(componentType, enumArray.length);
							for (int i = 0; i < enumArray.length; i++) {
								typedEnumArray[i] = (Enum<?>) enumArray[i];
							}
							@SuppressWarnings("unchecked")
							Class<Enum<?>> enumComponentType = (Class<Enum<?>>) componentType;
							annoBuilder = annoBuilder.defineEnumerationArray(entry.getKey(), enumComponentType,
									typedEnumArray);
						}
					} else {
						throw new IllegalStateException(
								format("Unsupported type %s for %s", annoValue.getClass(), annoValue));
					}
				}

				inner = inner.annotateParameter(annoBuilder.build());
			}
		}

		var builderSuf = inner == null ? builder : inner;
		var result = builderSuf.intercept(MethodCall.invoke(Object.class.getConstructor())).make();
		dump(result);
		return result;
	}

	public static Class<?> transform(Unloaded<Object> dynamicClass)
			throws NoSuchMethodException, ClassNotFoundException {
		var name = dynamicClass.getTypeDescription().getName();
		// parent is the SystemClassLoader so if the class depends on other classes that
		// the classes there, we fail (what is what we want here)
		var originalClassLoader = ClassLoader.getSystemClassLoader();

		var loadedClass = new ByteArrayClassLoader(originalClassLoader, singletonMap(name, dynamicClass.getBytes()),
				PersistenceHandler.MANIFEST).loadClass(name);
		var builder = new ByteBuddy().redefine(loadedClass);
		var transformed = sut.apply(builder, TypeDescription.ForLoadedType.of(loadedClass), null).make();
		dump(transformed);
		var transformedClassLoader = new ByteArrayClassLoader(originalClassLoader, emptyMap(),
				PersistenceHandler.MANIFEST);
		return transformed.load(transformedClassLoader, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
	}

	private static Unloaded<?> dump(Unloaded<?> transformed) {
		if (DEBUG) {
			try {
				transformed.saveIn(new File(System.getProperty("java.io.tmpdir")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return transformed;
	}

}
