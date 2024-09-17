package com.github.pfichtner.vaadoo.supplier;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.providers.TypeUsage;

public class CharSequences implements ArbitrarySupplier<CharSequence> {

	public static enum Type {
		NON_BLANKS("x", "xXx", "x ", " x"), BLANKS("", " ", "     ");

		private final CharSequence[] sequences;

		Type(String... values) {
			this.sequences = values;
		}

		public CharSequence[] sequences() {
			return sequences;
		}

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface Types {
		Type[] value();

		Class<?>[] ofType() default {};
	}

	@Override
	public Arbitrary<CharSequence> get() {
		return arbitraries(EnumSet.allOf(Type.class));
	}

	@Override
	public Arbitrary<CharSequence> supplyFor(TypeUsage targetType) {
		return arbitraries(targetType.findAnnotation(Types.class).map(Types::value).map(Set::of)
				.orElseGet(() -> EnumSet.allOf(Type.class)));
	}

	private Arbitrary<CharSequence> arbitraries(Set<Type> type) {
		return Arbitraries.of(type.stream().map(Type::sequences).map(Set::of).flatMap(Collection::stream).toList());
	}

}
