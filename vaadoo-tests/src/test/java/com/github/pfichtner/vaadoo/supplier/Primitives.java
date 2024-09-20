package com.github.pfichtner.vaadoo.supplier;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.providers.TypeUsage;

public class Primitives implements ArbitrarySupplier<Example> {

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface Types {
		Class<?>[] value();

		boolean not() default false;
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
	public Arbitrary<Example> get() {
		return arbitrariesFor(suppliers.keySet());
	}

	@Override
	public Arbitrary<Example> supplyFor(TypeUsage targetType) {
		var annotation = targetType.findAnnotation(Types.class);
		var not = annotation.map(Types::not).filter(Boolean.TRUE::equals).isPresent();
		var all = annotation //
				.map(Types::value).map(Set::of) //
				.orElseGet(suppliers::keySet);
		return arbitrariesFor(not ? negate(all) : all);
	}

	private Collection<Class<?>> negate(Collection<Class<?>> notAllowed) {
		return suppliers.keySet().stream().filter(not(notAllowed::contains)).collect(toSet());
	}

	private Arbitrary<Example> arbitrariesFor(Collection<Class<?>> classses) {
		return Arbitraries.of(classses).flatMap(c -> suppliers.get(c).map(t -> new Example(c, t)));
	}
}
