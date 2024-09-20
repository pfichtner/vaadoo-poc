package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.convertValue;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.ARRAYS;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.CHARSEQUENCES;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.COLLECTIONS;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.MAPS;
import static com.github.pfichtner.vaadoo.supplier.Example.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.NotEmpty;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class NotEmptyTest {

	@Property
	void oks(@ForAll(supplier = Classes.class) @Classes.Types({ CHARSEQUENCES, COLLECTIONS, MAPS,
			ARRAYS }) Example example) throws Exception {
		var config = randomConfigWith(
				entry(example.type(), "param", convertValue(example.value(), false)).withAnno(NotEmpty.class));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void noks(@ForAll(supplier = Classes.class) @Classes.Types({ CHARSEQUENCES, COLLECTIONS, MAPS,
			ARRAYS }) Example example) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(
				entry(example.type(), parameterName, convertValue(example.value(), true)).withAnno(NotEmpty.class));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " must not be empty", IllegalArgumentException.class);
	}

	@Property
	void customMessage(@ForAll(supplier = Classes.class) @Classes.Types({ CHARSEQUENCES, COLLECTIONS, MAPS,
			ARRAYS }) Example example, @ForAll String message) throws Exception {
		var config = randomConfigWith(entry(example.type(), "param", convertValue(example.value(), true))
				.withAnno(NotEmpty.class, Map.of("message", message)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, message, IllegalArgumentException.class);
	}

	@Property
	void invalidParameterType(@ForAll(supplier = Classes.class) @Classes.Types(not = true, value = { CHARSEQUENCES,
			COLLECTIONS, MAPS, ARRAYS }) Example example) throws NoSuchMethodException, ClassNotFoundException {
		var config = randomConfigWith(entry(example.type(), "param", nullValue()).withAnno(NotEmpty.class));
		assertThat(catchIllegalStateException(() -> transform(dynamicClass(config))))
				.hasMessageContainingAll(NotEmpty.class.getName(), "not allowed")
				.hasMessageContaining(example.type().getName());
	}

}
