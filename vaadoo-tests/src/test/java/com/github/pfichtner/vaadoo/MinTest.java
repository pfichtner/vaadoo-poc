package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.lowerBoundInLongRange;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.upperBoundInLongRange;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.NumberWrapper.numberWrapper;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.NUMBERS;
import static com.github.pfichtner.vaadoo.supplier.Example.nullValue;

import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;
import com.github.pfichtner.vaadoo.supplier.Primitives;

import jakarta.validation.constraints.Min;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class MinTest {

	@Property
	void primitiveValueLowerMin( //
			@ForAll(supplier = Primitives.class) //
			@Primitives.Types({ int.class, long.class, short.class, byte.class }) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		Assume.that(!value.isMin());
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(entry(value.type(), parameterName, value.sub(1)).withAnno(Min.class,
				Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " should be >= " + value.flooredLong(),
				IllegalArgumentException.class);
	}

	@Property
	void primitivesValueEqualMin( //
			@ForAll(supplier = Primitives.class) //
			@Primitives.Types({ int.class, long.class, short.class, byte.class }) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(entry(value.type(), parameterName, value.value()).withAnno(Min.class,
				Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void objectValueLowerMin( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(value = NUMBERS) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		Assume.that(!value.isMin());
		Number sub = value.sub(1);
		Assume.that(upperBoundInLongRange(sub));
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(
				entry(value.type(), parameterName, sub).withAnno(Min.class, Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, parameterName + " should be >= " + value.flooredLong(),
				IllegalArgumentException.class);
	}

	@Property
	void objectValueEqualMin( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(value = NUMBERS) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		Assume.that(lowerBoundInLongRange(value.value()));
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(entry(value.type(), parameterName, value.value()).withAnno(Min.class,
				Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void nullObjectIsOk( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(value = NUMBERS) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(entry(value.type(), parameterName, nullValue()).withAnno(Min.class,
				Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void objectValueGreaterMin( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(value = NUMBERS) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		Assume.that(!value.isMax());
		Assume.that(lowerBoundInLongRange(value.value()));
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(entry(value.type(), parameterName, value.add(1)).withAnno(Min.class,
				Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void primitiveValueGreaterMin( //
			@ForAll(supplier = Primitives.class) //
			@Primitives.Types({ int.class, long.class, short.class, byte.class }) //
			Example example //
	) throws Exception {
		var parameterName = "param";
		var value = numberWrapper(example.type(), example.value());
		Assume.that(!value.isMax());
		@SuppressWarnings("unchecked")
		var config = randomConfigWith(entry(value.type(), parameterName, value.add(1)).withAnno(Min.class,
				Map.of("value", value.flooredLong())));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

}
