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

	void nullCheck(Null nill, Object ref);

	void notNullCheck(NotNull notNull, Object ref);

	void notBlankCheck(NotBlank notBlank, CharSequence charSequence);

	void notEmpty(NotEmpty notEmpty, CharSequence charSequence);

	void notEmpty(NotEmpty notEmpty, Collection<?> collection);

	void notEmpty(NotEmpty notEmpty, Map<?, ?> map);

	void notEmpty(NotEmpty notEmpty, Object[] objects);

	void assertTrue(AssertTrue assertTrue, boolean value);

	void assertTrue(AssertTrue assertTrue, Boolean value);

	void assertFalse(AssertFalse assertFalse, boolean value);

	void assertFalse(AssertFalse assertFalse, Boolean value);

	void min(Min min, byte value);

	void min(Min min, short value);

	void min(Min min, int value);

	void min(Min min, long value);

	void min(Min min, Byte value);

	void min(Min min, Short value);

	void min(Min min, Integer value);

	void min(Min min, Long value);

	void min(Min min, BigInteger value);

	void min(Min min, BigDecimal value);

}