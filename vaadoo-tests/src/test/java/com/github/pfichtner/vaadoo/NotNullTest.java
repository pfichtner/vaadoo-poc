package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.Example.nullValue;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.NotNull;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class NotNullTest {

	private static final Class<? extends Annotation> ANNO_CLASS = NotNull.class;

	@Property
	void oks(@ForAll(supplier = Classes.class) Example example) throws Exception {
		var config = randomConfigWith(entry(example.type(), "param", example.value()).withAnno(ANNO_CLASS));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void noks(@ForAll(supplier = Classes.class) Example example) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(entry(example.type(), parameterName, nullValue()).withAnno(ANNO_CLASS));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " must not be null", NullPointerException.class);
	}

	@Property
	void customMessage(@ForAll(supplier = Classes.class) Example example, @ForAll String message) throws Exception {
		var config = randomConfigWith(
				entry(example.type(), "param", nullValue()).withAnno(ANNO_CLASS, Map.of("message", message)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, message, NullPointerException.class);
	}

}
