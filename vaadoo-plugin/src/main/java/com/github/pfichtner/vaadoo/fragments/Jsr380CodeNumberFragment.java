package com.github.pfichtner.vaadoo.fragments;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public interface Jsr380CodeNumberFragment {

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

	// -------------------------------------

	void check(Max max, byte value);

	void check(Max max, short value);

	void check(Max max, int value);

	void check(Max max, long value);

	void check(Max max, Byte value);

	void check(Max max, Short value);

	void check(Max max, Integer value);

	void check(Max max, Long value);

	void check(Max max, BigInteger value);

	void check(Max max, BigDecimal value);

	// -------------------------------------

	void check(Positive positive, byte value);

	void check(Positive positive, short value);

	void check(Positive positive, int value);

	void check(Positive positive, long value);

	void check(Positive positive, Byte value);

	void check(Positive positive, Short value);

	void check(Positive positive, Integer value);

	void check(Positive positive, Long value);

	void check(Positive positive, BigInteger value);

	void check(Positive positive, BigDecimal value);

	// -------------------------------------

	void check(PositiveOrZero positiveOrZero, byte value);

	void check(PositiveOrZero positiveOrZero, short value);

	void check(PositiveOrZero positiveOrZero, int value);

	void check(PositiveOrZero positiveOrZero, long value);

	void check(PositiveOrZero positiveOrZero, Byte value);

	void check(PositiveOrZero positiveOrZero, Short value);

	void check(PositiveOrZero positiveOrZero, Integer value);

	void check(PositiveOrZero positiveOrZero, Long value);

	void check(PositiveOrZero positiveOrZero, BigInteger value);

	void check(PositiveOrZero positiveOrZero, BigDecimal value);

	// -------------------------------------

	void check(Negative negative, byte value);

	void check(Negative negative, short value);

	void check(Negative negative, int value);

	void check(Negative negative, long value);

	void check(Negative negative, Byte value);

	void check(Negative negative, Short value);

	void check(Negative negative, Integer value);

	void check(Negative negative, Long value);

	void check(Negative negative, BigInteger value);

	void check(Negative negative, BigDecimal value);

	// -------------------------------------

	void check(NegativeOrZero negativeOrZero, byte value);

	void check(NegativeOrZero negativeOrZero, short value);

	void check(NegativeOrZero negativeOrZero, int value);

	void check(NegativeOrZero negativeOrZero, long value);

	void check(NegativeOrZero negativeOrZero, Byte value);

	void check(NegativeOrZero negativeOrZero, Short value);

	void check(NegativeOrZero negativeOrZero, Integer value);

	void check(NegativeOrZero negativeOrZero, Long value);

	void check(NegativeOrZero negativeOrZero, BigInteger value);

	void check(NegativeOrZero negativeOrZero, BigDecimal value);

	// -------------------------------------
}
