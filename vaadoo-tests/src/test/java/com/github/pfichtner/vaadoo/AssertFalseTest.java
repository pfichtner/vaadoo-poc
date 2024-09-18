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

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Primitives;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.AssertFalse;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class AssertFalseTest {

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

	private static void test(Example example) throws Exception {
		var parameterName = "param";
		var value = (boolean) example.value();
		var config = randomConfigWith(
				entry(casted(example.type(), Boolean.class), parameterName, value).withAnno(AssertFalse.class));
		var transformed = transform(dynamicClass(config));
		var execResult = provideExecException(transformed, config);
		if (value) {
			assertException(execResult, parameterName + " should be false", IllegalArgumentException.class);
		} else {
			assertNoException(execResult);
		}
	}

}
