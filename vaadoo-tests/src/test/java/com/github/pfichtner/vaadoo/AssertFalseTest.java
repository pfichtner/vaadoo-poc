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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;
import com.github.pfichtner.vaadoo.supplier.Primitives;

import jakarta.validation.constraints.AssertFalse;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class AssertFalseTest {

	private static final Class<? extends Annotation> ANNO_CLASS = AssertFalse.class;

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
		var config = randomConfigWith(entry(Boolean.class, parameterName, (Boolean) nullValue()).withAnno(ANNO_CLASS));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void customMessage(
			@ForAll(supplier = Classes.class) @Classes.Types(value = WRAPPERS, ofType = Boolean.class) Example example,
			@ForAll String message) throws Exception {
		var config = randomConfigWith(
				entry(example.type(), "param", true).withAnno(ANNO_CLASS, Map.of("message", message)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, message, IllegalArgumentException.class);
	}

	@Property
	void primitiveInvalidParameterType( //
			@ForAll(supplier = Primitives.class) //
			@Primitives.Types(not = true, value = boolean.class) //
			Example example) throws NoSuchMethodException, ClassNotFoundException {
		var config = randomConfigWith(entry(example.type(), "param", false).withAnno(ANNO_CLASS));
		assertThat(catchIllegalStateException(() -> transform(dynamicClass(config))))
				.hasMessageContainingAll(ANNO_CLASS.getName(), "not allowed")
				.hasMessageContaining(example.type().getName());
	}

	@Property
	void objectInvalidParameterType( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(not = true, ofType = Boolean.class) //
			Example example) throws NoSuchMethodException, ClassNotFoundException {
		var config = randomConfigWith(entry(example.type(), "param", Boolean.FALSE).withAnno(ANNO_CLASS));
		assertThat(catchIllegalStateException(() -> transform(dynamicClass(config))))
				.hasMessageContainingAll(ANNO_CLASS.getName(), "not allowed")
				.hasMessageContaining(example.type().getName());
	}

	private static void test(Example example) throws Exception {
		var parameterName = "param";
		var value = (boolean) example.value();
		var config = randomConfigWith(
				entry(casted(example.type(), Boolean.class), parameterName, value).withAnno(ANNO_CLASS));
		var transformed = transform(dynamicClass(config));
		var execResult = provideExecException(transformed, config);
		if (value) {
			assertException(execResult, parameterName + " must be false", IllegalArgumentException.class);
		} else {
			assertNoException(execResult);
		}
	}

}
