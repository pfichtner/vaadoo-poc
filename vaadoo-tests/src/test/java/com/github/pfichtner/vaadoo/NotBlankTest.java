package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.casted;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.CharSequences.Type.BLANKS;
import static com.github.pfichtner.vaadoo.supplier.CharSequences.Type.NON_BLANKS;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.CHARSEQUENCES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.CharSequences;
import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.NotBlank;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.WithNull;

class NotBlankTest {

	@Property
	void oks( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@ForAll(supplier = CharSequences.class) //
			@CharSequences.Types(NON_BLANKS) //
			CharSequence nonBlank //
	) throws Exception {
		var config = randomConfigWith(entry(example.type(), "param", nonBlank).withAnno(NotBlank.class));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void noks( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@WithNull //
			@ForAll(supplier = CharSequences.class) //
			@CharSequences.Types(BLANKS) //
			CharSequence blank //
	) throws Exception {
		var parameterName = "param";
		boolean stringIsNull = blank == null;
		var config = randomConfigWith(
				entry(casted(example.type(), CharSequence.class), parameterName, blank).withAnno(NotBlank.class));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, //
				parameterName + " must not be blank",
				stringIsNull ? NullPointerException.class : IllegalArgumentException.class);
	}

	@Property
	void customMessage( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@WithNull //
			@ForAll(supplier = CharSequences.class) //
			@CharSequences.Types(BLANKS) //
			CharSequence blank, //
			@ForAll String message) throws Exception {
		boolean stringIsNull = blank == null;
		var config = randomConfigWith(
				entry(example.type(), "param", blank).withAnno(NotBlank.class, Map.of("message", message)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, message,
				stringIsNull ? NullPointerException.class : IllegalArgumentException.class);
	}

	@Property
	void invalidParameterType( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(not = true, value = CHARSEQUENCES) //
			Example example //
	) throws NoSuchMethodException, ClassNotFoundException {
		var config = randomConfigWith(entry(example.type(), "param", "X").withAnno(NotBlank.class));
		assertThat(catchIllegalStateException(() -> transform(dynamicClass(config))))
				.hasMessageContainingAll(NotBlank.class.getName(), "not allowed")
				.hasMessageContaining(example.type().getName());
	}

}
