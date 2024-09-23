package com.github.pfichtner.vaadoo.fragments.impl;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.IDN;
import java.util.Collection;
import java.util.Map;

import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern.Flag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class JdkOnlyCodeFragment implements Jsr380CodeFragment {

	@Override
	public void check(Null nullAnno, Object ref) {
		if (ref != null) {
			throw new IllegalArgumentException(nullAnno.message());
		}
	}

	@Override
	public void check(NotNull notNull, Object ref) {
		if (ref == null) {
			throw new NullPointerException(notNull.message());
		}
	}

	@Override
	public void check(NotBlank notBlank, CharSequence charSequence) {
		if (charSequence == null) {
			throw new NullPointerException(notBlank.message());
		}
		if (charSequence.toString().trim().length() <= 0) {
			throw new IllegalArgumentException(notBlank.message());
		}
	}

	@Override
	public void check(jakarta.validation.constraints.Pattern pattern, CharSequence charSequence) {
		if (charSequence != null) {
			Flag[] flags = pattern.flags();
			int flagValue = 0;
			for (int i = 0; i < flags.length; i++) {
				jakarta.validation.constraints.Pattern.Flag flag = flags[i];
				flagValue |= flag.getValue();
			}
			// TODO this should be optimized by converting this into private static final
			// field, beside to optimization it would be a fail fast for invalid regular expression
			if (!compile(pattern.regexp(), flagValue).matcher(charSequence).matches()) {
				throw new IllegalArgumentException(pattern.message());
			}
		}
	}

	@Override
	public void check(Email email, CharSequence charSequence) {
		if (charSequence != null && charSequence.length() != 0) {
			String stringValue = charSequence.toString();
			int splitPosition = stringValue.lastIndexOf('@');
			if (splitPosition < 0) {
				throw new IllegalArgumentException(email.message());
			}

			String localPart = stringValue.substring(0, splitPosition);
			if (localPart.length() > 64 || !compile("(?:" + "[a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uFFFF-]" + "+|\""
					+ "(?:[a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uFFFF-]|\\\\\\\\|\\\\\\\")" + "+\")"
					+ "(?:\\." + "(?:" + "[a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uFFFF-]" + "+|\""
					+ "(?:[a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uFFFF-]|\\\\\\\\|\\\\\\\")" + "+\")" + ")*",
					CASE_INSENSITIVE).matcher(localPart).matches()) {
				throw new IllegalArgumentException(email.message());
			}

			String domainPart = stringValue.substring(splitPosition + 1);
			boolean validEmailDomainAddress = false;
			try {
				validEmailDomainAddress = !domainPart.endsWith(".") && IDN.toASCII(domainPart).length() <= 255
						&& compile("(?:" + "[a-z\u0080-\uFFFF0-9!#$%&'*+/=?^_`{|}~]" + "-*)*"
								+ "[a-z\u0080-\uFFFF0-9!#$%&'*+/=?^_`{|}~]" + "+" + "+(?:\\." + "(?:"
								+ "[a-z\u0080-\uFFFF0-9!#$%&'*+/=?^_`{|}~]" + "-*)*"
								+ "[a-z\u0080-\uFFFF0-9!#$%&'*+/=?^_`{|}~]" + "+" + "+)*" + "|\\["
								+ "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}" + "\\]|" + "\\[IPv6:"
								+ "(?:(?:[0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,7}:|(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}|(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}|(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}|(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:(?:(?::[0-9a-fA-F]{1,4}){1,6})|:(?:(?::[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(?::[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(?:ffff(:0{1,4}){0,1}:){0,1}(?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])|(?:[0-9a-fA-F]{1,4}:){1,4}:(?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9]))"
								+ "\\]", CASE_INSENSITIVE).matcher(domainPart).matches();

			} catch (IllegalArgumentException e) {
			}
			if (!validEmailDomainAddress) {
				throw new IllegalArgumentException(email.message());
			}

			// additional check
			Flag[] flags = email.flags();
			int flagValue = 0;
			for (int i = 0; i < flags.length; i++) {
				jakarta.validation.constraints.Pattern.Flag flag = flags[i];
				flagValue |= flag.getValue();
			}
			if (!compile(email.regexp(), flagValue).matcher(charSequence).matches()) {
				throw new IllegalArgumentException(email.message());
			}
		}
	}

	@Override
	public void check(NotEmpty notEmpty, CharSequence charSequence) {
		if (charSequence == null) {
			throw new NullPointerException(notEmpty.message());
		}
		if (charSequence.length() <= 0) {
			throw new IllegalArgumentException(notEmpty.message());
		}
	}

	public void check(NotEmpty notEmpty, Collection<?> collection) {
		if (collection == null) {
			throw new NullPointerException(notEmpty.message());
		}
		if (collection.size() <= 0) {
			throw new IllegalArgumentException(notEmpty.message());
		}
	}

	@Override
	public void check(NotEmpty notEmpty, Map<?, ?> map) {
		if (map == null) {
			throw new NullPointerException(notEmpty.message());
		}
		if (map.size() <= 0) {
			throw new IllegalArgumentException(notEmpty.message());
		}
	}

	@Override
	public void check(NotEmpty notEmpty, Object[] objects) {
		if (objects == null) {
			throw new NullPointerException(notEmpty.message());
		}
		if (objects.length <= 0) {
			throw new IllegalArgumentException(notEmpty.message());
		}
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Size size, CharSequence charSequence) {
		if (charSequence != null && (charSequence.length() < size.min() || charSequence.length() > size.max())) {
			throw new IllegalArgumentException(size.message());
		}
	}

	public void check(Size size, Collection<?> collection) {
		if (collection != null && (collection.size() < size.min() || collection.size() > size.max())) {
			throw new IllegalArgumentException(size.message());
		}
	}

	@Override
	public void check(Size size, Map<?, ?> map) {
		if (map != null && (map.size() < size.min() || map.size() > size.max())) {
			throw new IllegalArgumentException(size.message());
		}
	}

	@Override
	public void check(Size size, Object[] objects) {
		if (objects != null && (objects.length < size.min() || objects.length > size.max())) {
			throw new IllegalArgumentException(size.message());
		}
	}

	// -----------------------------------------------------------------

	@Override
	public void check(AssertTrue assertTrue, boolean value) {
		if (!value) {
			throw new IllegalArgumentException(assertTrue.message());
		}
	}

	@Override
	public void check(AssertTrue assertTrue, Boolean value) {
		if (value != null && !value) {
			throw new IllegalArgumentException(assertTrue.message());
		}
	}

	@Override
	public void check(AssertFalse assertFalse, boolean value) {
		if (value) {
			throw new IllegalArgumentException(assertFalse.message());
		}
	}

	@Override
	public void check(AssertFalse assertFalse, Boolean value) {
		if (value != null && value) {
			throw new IllegalArgumentException(assertFalse.message());
		}
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Min min, byte value) {
		if (value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, short value) {
		if (value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, int value) {
		if (value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, long value) {
		if (value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, Byte value) {
		if (value != null && value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, Short value) {
		if (value != null && value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, Integer value) {
		if (value != null && value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, Long value) {
		if (value != null && value < min.value()) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, BigInteger value) {
		if (value != null && value.compareTo(BigInteger.valueOf(min.value())) < 0) {
			throw new IllegalArgumentException(min.message());
		}
	}

	@Override
	public void check(Min min, BigDecimal value) {
		if (value != null && value.compareTo(BigDecimal.valueOf(min.value())) < 0) {
			throw new IllegalArgumentException(min.message());
		}
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Max max, byte value) {
		if (value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, short value) {
		if (value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, int value) {
		if (value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, long value) {
		if (value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, Byte value) {
		if (value != null && value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, Short value) {
		if (value != null && value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, Integer value) {
		if (value != null && value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, Long value) {
		if (value != null && value > max.value()) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, BigInteger value) {
		if (value != null && value.compareTo(BigInteger.valueOf(max.value())) > 0) {
			throw new IllegalArgumentException(max.message());
		}
	}

	@Override
	public void check(Max max, BigDecimal value) {
		if (value != null && value.compareTo(BigDecimal.valueOf(max.value())) > 0) {
			throw new IllegalArgumentException(max.message());
		}
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Positive positive, byte value) {
		if (value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, short value) {
		if (value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, int value) {
		if (value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, long value) {
		if (value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, Byte value) {
		if (value != null && value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, Short value) {
		if (value != null && value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, Integer value) {
		if (value != null && value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, Long value) {
		if (value != null && value <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, BigInteger value) {
		if (value != null && value.signum() <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	@Override
	public void check(Positive positive, BigDecimal value) {
		if (value != null && value.signum() <= 0) {
			throw new IllegalArgumentException(positive.message());
		}
	}

	// -----------------------------------------------------------------
	@Override
	public void check(PositiveOrZero positiveOrZero, byte value) {
		if (value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, short value) {
		if (value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, int value) {
		if (value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, long value) {
		if (value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Byte value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Short value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Integer value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Long value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, BigInteger value) {
		if (value != null && value.signum() < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, BigDecimal value) {
		if (value != null && value.signum() < 0) {
			throw new IllegalArgumentException(positiveOrZero.message());
		}
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Negative negative, byte value) {
		if (value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, short value) {
		if (value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, int value) {
		if (value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, long value) {
		if (value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, Byte value) {
		if (value != null && value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, Short value) {
		if (value != null && value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, Integer value) {
		if (value != null && value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, Long value) {
		if (value != null && value >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, BigInteger value) {
		if (value != null && value.signum() >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}

	@Override
	public void check(Negative negative, BigDecimal value) {
		if (value != null && value.signum() >= 0) {
			throw new IllegalArgumentException(negative.message());
		}
	}
	// -----------------------------------------------------------------

	@Override
	public void check(NegativeOrZero negativeOrZero, byte value) {
		if (value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, short value) {
		if (value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, int value) {
		if (value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, long value) {
		if (value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Byte value) {
		if (value != null && value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Short value) {
		if (value != null && value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Integer value) {
		if (value != null && value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Long value) {
		if (value != null && value > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, BigInteger value) {
		if (value != null && value.signum() > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, BigDecimal value) {
		if (value != null && value.signum() > 0) {
			throw new IllegalArgumentException(negativeOrZero.message());
		}
	}

}
