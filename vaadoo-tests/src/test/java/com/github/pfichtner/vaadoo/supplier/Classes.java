package com.github.pfichtner.vaadoo.supplier;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.emptySet;
import static java.util.EnumSet.allOf;
import static java.util.Map.entry;
import static java.util.function.Predicate.not;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;
import net.jqwik.api.providers.TypeUsage;

public class Classes implements ArbitrarySupplier<Tuple2<Class<?>, Object>> {

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface Types {
		SubTypes[] value();

		Class<?>[] ofType() default {};
	}

	public static enum SubTypes {
		OBJECT(Object.class), //
		WRAPPERS(Boolean.class, Integer.class, Long.class, Double.class, Float.class, Short.class, Byte.class,
				Character.class),
		LISTS(List.class, ArrayList.class, LinkedList.class, CopyOnWriteArrayList.class), //
		SETS(Set.class, HashSet.class, LinkedHashSet.class), //
		COLLECTIONS(Collection.class), //
		MAPS(Map.class, HashMap.class, LinkedHashMap.class, ConcurrentMap.class, ConcurrentHashMap.class), //
		CHARSEQUENCES(CharSequence.class, String.class), //
		NUMBERS(Integer.class, Long.class, Short.class, Byte.class, BigDecimal.class, BigInteger.class), //
		ARRAYS(Object[].class, Boolean[].class, Integer[].class, Long[].class, Double[].class, Float[].class,
				Short[].class, Character[].class);

		private final List<Class<?>> types;

		private SubTypes(Class<?>... types) {
			this.types = List.of(types);
		}

		private List<Class<?>> types() {
			return types;
		}

	}

	private static final Map<Class<?>, Arbitrary<?>> suppliers = Map.ofEntries( //
			entry(CharSequence.class, Arbitraries.strings()), //
			entry(Boolean.class, Arbitraries.of(Boolean.TRUE, Boolean.FALSE)), //
			entry(Integer.class, Arbitraries.integers()), //
			entry(Long.class, Arbitraries.longs()), //
			entry(Double.class, Arbitraries.doubles()), //
			entry(Float.class, Arbitraries.floats()), //
			entry(Short.class, Arbitraries.shorts()), //
			entry(Byte.class, Arbitraries.bytes()), //
			entry(Character.class, Arbitraries.chars()), //
			entry(BigInteger.class, Arbitraries.bigIntegers()), //
			entry(BigDecimal.class, Arbitraries.bigDecimals()) //
	);

	private List<Class<?>> allClasses(Set<SubTypes> subTypes) {
		return subTypes.stream().map(SubTypes::types).flatMap(Collection::stream).toList();
	}

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> get() {
		var allowed = allClasses(allOf(SubTypes.class)).stream().toList();
		return arbitraries(allowed);
	}

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> supplyFor(TypeUsage targetType) {
		var annotation = targetType.findAnnotation(Types.class);
		var onlyTheseTypesAreAllowd = annotation.map(Types::value).map(Set::of).orElseGet(() -> allOf(SubTypes.class));
		var allowedSuperTypes = onlyTheseTypesAreAllowd.stream().map(SubTypes::types).flatMap(Collection::stream)
				.toList();
		var allowed = allClasses(allOf(SubTypes.class)).stream().filter(filter(annotation))
				.filter(c -> isSubtypeOfOneOf(c, allowedSuperTypes)).toList();
		return arbitraries(allowed);
	}

	private static Predicate<Class<?>> filter(Optional<Types> annotation) {
		var only = annotation.map(Types::ofType).map(Set::of).orElse(emptySet());
		return only.isEmpty() ? c -> true : only::contains;
	}

	private static Arbitrary<Tuple2<Class<?>, Object>> arbitraries(List<Class<?>> allowed) {
		return Arbitraries.of(allowed).flatMap(c -> supplierFor(c, allowed).map(t -> Tuple.of(c, t)));
	}

	private boolean isSubtypeOfOneOf(Class<?> c, List<Class<?>> allowedSuperTypes) {
		return allowedSuperTypes.stream().anyMatch(s -> s.isAssignableFrom(c));
	}

	private static Arbitrary<?> supplierFor(Class<?> clazz, List<Class<?>> matchingTypes) {
		var arbitrary = suppliers.get(clazz);
		return arbitrary == null //
				? Arbitraries.of(instantiables(clazz, matchingTypes)).map(Classes::newInstance) //
				: arbitrary;
	}

	private static List<Class<?>> instantiables(Class<?> clazz, List<Class<?>> matchingTypes) {
		List<Class<?>> list = matchingTypes.stream() //
				.filter(t -> clazz.isAssignableFrom(t)) //
				.filter(not(Class::isInterface)) //
				.filter(Classes::canCreate) //
				.toList();
		assert !list.isEmpty() : "no instantiables for type " + clazz;
		return list;
	}

	private static boolean canCreate(Class<?> clazz) {
		return clazz.isArray() || hasNoArgConstructor(clazz);
	}

	private static boolean hasNoArgConstructor(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredConstructors()).anyMatch(p -> p.getParameters().length == 0);
	}

	private static Object newInstance(Class<?> clazz) {
		if (clazz.isArray()) {
			return Array.newInstance(clazz.componentType(), 0);
		}
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
