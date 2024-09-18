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
				parameterName + " must not be " + (stringIsNull ? "null" : "blank"),
				stringIsNull ? NullPointerException.class : IllegalArgumentException.class);
	}

}
