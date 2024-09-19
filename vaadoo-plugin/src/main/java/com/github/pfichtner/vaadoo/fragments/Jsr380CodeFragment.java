package com.github.pfichtner.vaadoo.fragments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

public interface Jsr380CodeFragment {

	void check(Null nullAnno, Object ref);

	void check(NotNull notNull, Object ref);

	void check(NotBlank notBlank, CharSequence charSequence);

	void check(NotEmpty notEmpty, CharSequence charSequence);

	void check(NotEmpty notEmpty, Collection<?> collection);

	void check(NotEmpty notEmpty, Map<?, ?> map);

	void check(NotEmpty notEmpty, Object[] objects);

	void check(AssertTrue assertTrue, boolean value);

	void check(AssertTrue assertTrue, Boolean value);

	void check(AssertFalse assertFalse, boolean value);

	void check(AssertFalse assertFalse, Boolean value);

	void check(Min min, byte value);

	void check(Min min, short value);

	void check(Min min, int value);

	void check(Min min, long value);

	void check(Min min, Byte value);

	void check(Min min, Short value);

	void check(Min min, Integer value);

	void check(Min min, Long value);

	void check(Min min, BigInteger value);

	void check(Min min, BigDecimal value);

}