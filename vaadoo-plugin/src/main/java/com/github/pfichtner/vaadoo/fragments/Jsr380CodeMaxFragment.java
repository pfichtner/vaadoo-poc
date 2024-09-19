package com.github.pfichtner.vaadoo.fragments;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.validation.constraints.Max;

public interface Jsr380CodeMaxFragment {

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

}
