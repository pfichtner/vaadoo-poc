package com.github.pfichtner.vaadoo.supplier;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

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
		Class<?>[] value();
	}

	Map<Class<?>, Arbitrary<?>> primitives = Map.of( //
			int.class, Arbitraries.integers(), //
			long.class, Arbitraries.longs(), //
			double.class, Arbitraries.doubles(), //
			float.class, Arbitraries.floats(), //
			short.class, Arbitraries.shorts(), //
			char.class, Arbitraries.chars(), //
			boolean.class, Arbitraries.of(true, false) //
	);

	Map<Class<?>, Arbitrary<?>> wrappers = Map.of( //
			Integer.class, Arbitraries.integers(), //
			Long.class, Arbitraries.longs(), //
			Double.class, Arbitraries.doubles(), //
			Float.class, Arbitraries.floats(), //
			Short.class, Arbitraries.shorts(), //
			Character.class, Arbitraries.chars(), //
			Boolean.class, Arbitraries.of(Boolean.TRUE, Boolean.FALSE) //
	);

	Map<Class<?>, Arbitrary<?>> collections = Map.of( //
			List.class, Arbitraries.of(new ArrayList<>(), new LinkedList<>()), //
			Set.class, Arbitraries.of(new HashSet<>(), new LinkedHashSet<>()) //
	);

	Map<Class<?>, Arbitrary<?>> collection = Map.of( //
			Collection.class, Arbitraries.oneOf(collections.values()) //
	);

	Map<Class<?>, Arbitrary<?>> maps = Map.of( //
			Map.class, Arbitraries.of(new HashMap<>(), new LinkedHashMap<>()) //
	);

	Map<Class<?>, Arbitrary<?>> charSequences = Map.of( //
			CharSequence.class, Arbitraries.strings().ofMaxLength(3), //
			String.class, Arbitraries.strings().ofMaxLength(3) //
	);

	Map<Class<?>, Arbitrary<?>> allArbitraries = Stream.of(
//			  primitives, 
			wrappers, collections, collection, charSequences) //
			.map(Map::entrySet) //
			.flatMap(Set::stream) //
			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> get() {
		List<Arbitrary<Tuple2<Class<?>, Object>>> tupleArbitraries = new ArrayList<>();
		for (Map.Entry<Class<?>, Arbitrary<?>> entry : allArbitraries.entrySet()) {
			tupleArbitraries.add(entry.getValue().map(value -> Tuple.of(entry.getKey(), value)));
		}
		return Arbitraries.oneOf(tupleArbitraries);
	}

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> supplyFor(TypeUsage targetType) {
		var targetClass = targetType.findAnnotation(Types.class).map(Types::value);
		return targetClass.map(this::filteredArbitraries).orElseGet(this::get);
	}

	private Arbitrary<Tuple2<Class<?>, Object>> filteredArbitraries(Class<?>[] xxx) {
		var filteredArbitraries = allArbitraries.entrySet().stream() //
				.filter(e -> Arrays.stream(xxx).anyMatch(t -> t.isAssignableFrom(e.getKey()))) //
				.map(Classes::arbitrary).collect(toList());
		return Arbitraries.oneOf(filteredArbitraries);
	}

	private static Arbitrary<Tuple2<Class<?>, Object>> arbitrary(Entry<Class<?>, Arbitrary<?>> entry) {
		return entry.getValue().map(value -> Tuple.of(entry.getKey(), value));
	}

}
