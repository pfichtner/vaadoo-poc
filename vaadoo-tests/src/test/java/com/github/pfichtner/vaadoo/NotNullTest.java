package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.NotNull;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class NotNullTest {

	@Property
	void oks(@ForAll(supplier = Classes.class) Example tuple) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(entry(tuple.type(), parameterName, null).withAnno(NotNull.class));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " must not be null", NullPointerException.class);
	}

	@Property
	void noks(@ForAll(supplier = Classes.class) Example tuple) throws Exception {
		var config = randomConfigWith(entry(tuple.type(), "param", tuple.value()).withAnno(NotNull.class));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

}
