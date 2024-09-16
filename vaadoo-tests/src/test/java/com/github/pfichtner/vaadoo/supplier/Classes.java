package com.github.pfichtner.vaadoo.supplier;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.function.Predicate.not;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	}

	public static enum SubTypes {
		OBJECT(Object.class), //
		WRAPPERS(Boolean.class, Integer.class, Long.class, Double.class, Float.class, Short.class, Character.class),
		LISTS(List.class, ArrayList.class, LinkedList.class), SETS(Set.class, HashSet.class, LinkedHashSet.class), //
		COLLECTIONS(Collection.class), //
		MAPS(Map.class, HashMap.class, LinkedHashMap.class), //
		CHARSEQUENCES(CharSequence.class, String.class), //
		ARRAYS(Object[].class, Boolean[].class, Integer[].class, Long[].class, Double[].class, Float[].class,
				Short[].class, Character[].class);

		private final List<Class<?>> types;

		private SubTypes(Class<?>... types) {
			this.types = List.of(types);
		}

		public List<Class<?>> types() {
			return types;
		}

	}

	private static final Map<Class<?>, Arbitrary<?>> suppliers = Map.of( //
			CharSequence.class, Arbitraries.strings(), //
			Boolean.class, Arbitraries.of(Boolean.TRUE, Boolean.FALSE), //
			Integer.class, Arbitraries.integers(), //
			Long.class, Arbitraries.longs(), //
			Double.class, Arbitraries.doubles(), //
			Float.class, Arbitraries.floats(), //
			Short.class, Arbitraries.shorts(), //
			Character.class, Arbitraries.chars() //
	);

	private List<Class<?>> all(Set<SubTypes> sub) {
		return sub.stream().map(SubTypes::types).flatMap(Collection::stream).toList();
	}

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> get() {
		var all = all(EnumSet.allOf(SubTypes.class)).stream().toList();
		return Arbitraries.of(all).flatMap(c -> supplierFor(c, all).map(t -> Tuple.of(c, t)));
	}

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> supplyFor(TypeUsage targetType) {
		Set<SubTypes> onlyTheseTypesAreAllowd = targetType.findAnnotation(Types.class).map(Types::value).map(Set::of)
				.orElseGet(() -> EnumSet.allOf(SubTypes.class));
		var allowedSuperTypes = onlyTheseTypesAreAllowd.stream().map(SubTypes::types).flatMap(Collection::stream)
				.toList();
		var allowed = all(EnumSet.allOf(SubTypes.class)).stream().filter(c -> isSubtypeOfOneOf(c, allowedSuperTypes))
				.toList();
		return Arbitraries.of(allowed).flatMap(c -> supplierFor(c, allowed).map(t -> Tuple.of(c, t)));
	}

	private boolean isSubtypeOfOneOf(Class<?> c, List<Class<?>> allowedSuperTypes) {
		return allowedSuperTypes.stream().anyMatch(s -> s.isAssignableFrom(c));
	}

	private Arbitrary<?> supplierFor(Class<?> clazz, List<Class<?>> matchingTypes) {
		var arbitrary = suppliers.get(clazz);
		return arbitrary == null //
				? Arbitraries.of(matchingTypes.stream() //
						.filter(t -> clazz.isAssignableFrom(t)) //
						.filter(not(Class::isInterface)) //
						.filter(Classes::canCreate) //
						.toList()) //
						.map(Classes::newInstance) //
				: arbitrary;
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
