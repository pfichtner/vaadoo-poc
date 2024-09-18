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

public interface CodeFragment {

	void verify(Null nill, Object ref);

	void verify(NotNull notNull, Object ref);

	void verify(NotBlank notBlank, CharSequence charSequence);

	void verify(NotEmpty notEmpty, CharSequence charSequence);

	void verify(NotEmpty notEmpty, Collection<?> collection);

	void verify(NotEmpty notEmpty, Map<?, ?> map);

	void verify(NotEmpty notEmpty, Object[] objects);

	void verify(AssertTrue assertTrue, boolean value);

	void verify(AssertTrue assertTrue, Boolean value);

	void verify(AssertFalse assertFalse, boolean value);

	void verify(AssertFalse assertFalse, Boolean value);

	void verify(Min min, byte value);

	void verify(Min min, short value);

	void verify(Min min, int value);

	void verify(Min min, long value);

	void verify(Min min, Byte value);

	void verify(Min min, Short value);

	void verify(Min min, Integer value);

	void verify(Min min, Long value);

	void verify(Min min, BigInteger value);

	void verify(Min min, BigDecimal value);

}