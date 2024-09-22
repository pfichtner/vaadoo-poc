package com.github.pfichtner.vaadoo.fragments;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.validation.constraints.Min;

public interface Jsr380CodeMinFragment {

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
