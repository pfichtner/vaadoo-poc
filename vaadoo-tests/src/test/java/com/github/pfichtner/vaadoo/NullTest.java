package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.Example.nullValue;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.Null;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class NullTest {

	@Property
	void oks(@ForAll(supplier = Classes.class) Example example) throws Exception {
		var config = randomConfigWith(entry(example.type(), "param", nullValue()).withAnno(Null.class));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void noks(@ForAll(supplier = Classes.class) Example example) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(entry(example.type(), parameterName, example.value()).withAnno(Null.class));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " expected to be null", IllegalArgumentException.class);
	}

}
