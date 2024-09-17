package com.github.pfichtner.vaadoo.supplier;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;
import net.jqwik.api.providers.TypeUsage;

public class Primitives implements ArbitrarySupplier<Tuple2<Class<?>, Object>> {

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface Types {
		Class<?>[] value();
	}

	private static final Map<Class<?>, Arbitrary<?>> suppliers = Map.of( //
			boolean.class, Arbitraries.of(true, false), //
			int.class, Arbitraries.integers(), //
			long.class, Arbitraries.longs(), //
			double.class, Arbitraries.doubles(), //
			float.class, Arbitraries.floats(), //
			short.class, Arbitraries.shorts(), //
			byte.class, Arbitraries.bytes(), //
			char.class, Arbitraries.chars() //
	);

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> get() {
		return arbitrariesFor(suppliers.keySet());
	}

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> supplyFor(TypeUsage targetType) {
		return arbitrariesFor(targetType.findAnnotation(Types.class) //
				.map(Types::value).map(Set::of) //
				.orElseGet(suppliers::keySet));
	}

	private Arbitrary<Tuple2<Class<?>, Object>> arbitrariesFor(Set<Class<?>> classses) {
		return Arbitraries.of(classses).flatMap(c -> suppliers.get(c).map(t -> Tuple.of(c, t)));
	}
}
