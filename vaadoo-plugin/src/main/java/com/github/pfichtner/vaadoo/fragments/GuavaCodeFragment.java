package com.github.pfichtner.vaadoo.fragments;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

public class GuavaCodeFragment implements CodeFragment {

	private static final String NOT_NULL = "%s must not be null";
	private static final String NOT_BLANK = "%s must not be blank";
	private static final String NOT_EMPTY = "%s must not be empty";
	private static final String SHOULD_BE_TRUE = "%s should be true";
	private static final String SHOULD_BE_FALSE = "%s should be false";

	@Override
	public void nullCheck(Null nill, Object ref) {
		checkArgument(ref == null, "%s expected to be null");
	}

	@Override
	public void notNullCheck(NotNull notNull, Object ref) {
		checkNotNull(ref, NOT_NULL);
	}

	@Override
	public void notBlankCheck(NotBlank notBlank, CharSequence charSequence) {
		checkArgument(whitespace().negate().countIn(checkNotNull(charSequence, NOT_NULL)) > 0, NOT_BLANK);
	}

	@Override
	public void notEmpty(NotEmpty notEmpty, CharSequence charSequence) {
		checkArgument(checkNotNull(charSequence, NOT_NULL).length() > 0, NOT_EMPTY);
	}

	public void notEmpty(NotEmpty notEmpty, Collection<?> collection) {
		checkArgument(checkNotNull(collection, NOT_NULL).size() > 0, NOT_EMPTY);
	}

	@Override
	public void notEmpty(NotEmpty notEmpty, Map<?, ?> map) {
		checkArgument(checkNotNull(map, NOT_NULL).size() > 0, NOT_EMPTY);
	}

	@Override
	public void notEmpty(NotEmpty notEmpty, Object[] objects) {
		checkArgument(checkNotNull(objects, NOT_NULL).length > 0, NOT_EMPTY);
	}

	@Override
	public void assertTrue(AssertTrue assertTrue, boolean value) {
		checkArgument(value, SHOULD_BE_TRUE);
	}

	@Override
	public void assertTrue(AssertTrue assertTrue, Boolean value) {
		checkArgument(value == null || value, SHOULD_BE_TRUE);
	}

	@Override
	public void assertFalse(AssertFalse assertFalse, boolean value) {
		checkArgument(!value, SHOULD_BE_FALSE);
	}

	@Override
	public void assertFalse(AssertFalse assertFalse, Boolean value) {
		checkArgument(value == null || !value, SHOULD_BE_FALSE);
	}

	@Override
	public void min(Min min, byte value) {
		checkArgument(value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, short value) {
		checkArgument(value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, int value) {
		checkArgument(value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, long value) {
		checkArgument(value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, Byte value) {
		checkArgument(value == null || value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, Short value) {
		checkArgument(value == null || value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, Integer value) {
		checkArgument(value == null || value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, Long value) {
		checkArgument(value == null || value >= min.value(), "%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, BigInteger value) {
		checkArgument(value == null || value.compareTo(BigInteger.valueOf(min.value())) >= 0,
				"%s should be >= " + min.value());
	}

	@Override
	public void min(Min min, BigDecimal value) {
		checkArgument(value == null || value.compareTo(BigDecimal.valueOf(min.value())) >= 0,
				"%s should be >= " + min.value());
	}

}
