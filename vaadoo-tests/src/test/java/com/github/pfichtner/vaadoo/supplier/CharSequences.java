package com.github.pfichtner.vaadoo.supplier;

import static com.github.pfichtner.vaadoo.supplier.CharSequences.Type.BLANKS;
import static com.github.pfichtner.vaadoo.supplier.CharSequences.Type.NON_BLANKS;
import static com.google.common.collect.Streams.concat;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.EnumSet.allOf;
import static java.util.stream.Collectors.joining;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;
import net.jqwik.api.providers.TypeUsage;

public class CharSequences implements ArbitrarySupplier<CharSequence> {

	private static enum SequenceChunks {
		BLANKS("", " ", "\t", "\n", "\r", "\t \n", "\r\n "), //
		NON_BLANKS("x", "X", "x ", " x", "X ", " X", "1", "!", "-", "_", "/", "abc", "XyZ-987");

		private final CharSequence[] sequences;

		private SequenceChunks(CharSequence... values) {
			this.sequences = values;
		}

		private CharSequence[] sequences() {
			return sequences;
		}

	}

	public static enum Type {
		BLANKS, NON_BLANKS;
	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface Types {
		Type[] value();
	}

	@Override
	public Arbitrary<CharSequence> get() {
		return arbitraries(allOf(Type.class));
	}

	@Override
	public Arbitrary<CharSequence> supplyFor(TypeUsage targetType) {
		return arbitraries(targetType.findAnnotation(Types.class) //
				.map(Types::value).map(Set::of) //
				.orElseGet(() -> allOf(Type.class)));
	}

	private static Arbitrary<CharSequence> arbitraries(Set<Type> types) {
		if (types.contains(Type.NON_BLANKS)) {
			return generateNonBlankArbitrary();
		} else if (types.contains(BLANKS)) {
			return generateBlankArbitrary();
		}
		throw new IllegalStateException("types has to contain " + BLANKS + " or " + NON_BLANKS);
	}

	private static Arbitrary<CharSequence> generateNonBlankArbitrary() {
		var blanksArbitrary = Arbitraries.of(SequenceChunks.BLANKS.sequences()).list().ofMinSize(1).ofMaxSize(10);
		var nonBlanksArbitrary = Arbitraries.of(SequenceChunks.NON_BLANKS.sequences()).list().ofMinSize(1).ofMinSize(3);
		return blanksArbitrary.flatMap(blanks -> nonBlanksArbitrary
				.map(nonBlanks -> concat(blanks.stream(), nonBlanks.stream()).collect(joining())));
	}

	private static Arbitrary<CharSequence> generateBlankArbitrary() {
		return Arbitraries.of(SequenceChunks.BLANKS.sequences()).list().ofMinSize(1)
				.map(list -> list.stream().collect(joining()));
	}

}
