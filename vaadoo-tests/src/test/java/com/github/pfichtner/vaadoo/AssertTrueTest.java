package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.casted;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.provideExecException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.WRAPPERS;
import static com.github.pfichtner.vaadoo.supplier.Example.nullValue;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;
import com.github.pfichtner.vaadoo.supplier.Primitives;

import jakarta.validation.constraints.AssertTrue;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class AssertTrueTest {

	@Property
	void primitives(@ForAll(supplier = Primitives.class) @Primitives.Types(boolean.class) Example example)
			throws Exception {
		test(example);
	}

	@Property
	void wrappers(
			@ForAll(supplier = Classes.class) @Classes.Types(value = WRAPPERS, ofType = Boolean.class) Example example)
			throws Exception {
		test(example);
	}

	@Property
	void nullObjectIsOk(
			@ForAll(supplier = Classes.class) @Classes.Types(value = WRAPPERS, ofType = Boolean.class) Example example)
			throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(
				entry(Boolean.class, parameterName, (Boolean) nullValue()).withAnno(AssertTrue.class));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	private static void test(Example example) throws Exception {
		var parameterName = "param";
		var value = (boolean) example.value();
		var config = randomConfigWith(
				entry(casted(example.type(), Boolean.class), parameterName, value).withAnno(AssertTrue.class));
		var transformed = transform(dynamicClass(config));
		var execResult = provideExecException(transformed, config);
		if (value) {
			assertNoException(execResult);
		} else {
			assertException(execResult, parameterName + " should be true", IllegalArgumentException.class);
		}
	}

}
