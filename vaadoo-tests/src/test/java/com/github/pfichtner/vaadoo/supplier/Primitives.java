package com.github.pfichtner.vaadoo.supplier;

import java.util.Map;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;

public class Primitives implements ArbitrarySupplier<Tuple2<Class<?>, Object>> {

	private static final Map<Class<?>, Arbitrary<?>> suppliers = Map.of( //
			boolean.class, Arbitraries.of(true, false), //
			int.class, Arbitraries.integers(), //
			long.class, Arbitraries.longs(), //
			double.class, Arbitraries.doubles(), //
			float.class, Arbitraries.floats(), //
			short.class, Arbitraries.shorts(), //
			char.class, Arbitraries.chars() //
	);

	@Override
	public Arbitrary<Tuple2<Class<?>, Object>> get() {
		return Arbitraries.of(suppliers.keySet()).map(c -> Tuple.of(c, suppliers.get(c)));
	}

}
