package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.CHARSEQUENCES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.Pattern;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.WithNull;

class PatternTest {

	private static final Class<? extends Annotation> ANNO_CLASS = Pattern.class;

	private static final String DIGIT_ONLY_PATTERN = "\\d+";

	@Property
	void oks( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@WithNull @ForAll @IntRange(min = 0) Integer digit //
	) throws Exception {
		var config = randomConfigWith(entry(example.type(), "param", digitString(digit)).withAnno(ANNO_CLASS,
				Map.of("regexp", DIGIT_ONLY_PATTERN, "flags", new Pattern.Flag[] { Pattern.Flag.CASE_INSENSITIVE })));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void noks( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@ForAll @IntRange(min = 0) Integer digit //
	) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(entry(example.type(), parameterName, digitString(digit) + "X")
				.withAnno(ANNO_CLASS, Map.of("regexp", DIGIT_ONLY_PATTERN)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " must match \"\\d+\"", IllegalArgumentException.class);
	}

	@Property
	void customMessage( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@ForAll String message, //
			@ForAll Pattern.Flag[] flags //
	) throws Exception {
		Assume.that(!example.value().toString().isEmpty());
		var config = randomConfigWith(entry(example.type(), "param", "X").withAnno(ANNO_CLASS,
				Map.of("regexp", DIGIT_ONLY_PATTERN, "message", message, "flags", flags)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, message, IllegalArgumentException.class);
	}

	@Property
	void invalidParameterType( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(not = true, value = CHARSEQUENCES) //
			Example example, //
			@ForAll Pattern.Flag[] flags //
	) throws NoSuchMethodException, ClassNotFoundException {
		var config = randomConfigWith(
				entry(example.type(), "param", "X").withAnno(ANNO_CLASS, Map.of("regexp", ".*", "flags", flags)));
		assertThat(catchIllegalStateException(() -> transform(dynamicClass(config))))
				.hasMessageContainingAll(ANNO_CLASS.getName(), "not allowed")
				.hasMessageContaining(example.type().getName());
	}

	private static String digitString(Integer digit) {
		return digit == null ? null : String.valueOf(digit);
	}

}
