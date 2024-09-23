package com.github.pfichtner.vaadoo.fragments.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
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

public class GuavaCodeFragment implements Jsr380CodeFragment {

	@Override
	public void check(Null nullAnno, Object ref) {
		checkArgument(ref == null, nullAnno.message());
	}

	@Override
	public void check(NotNull notNull, Object ref) {
		checkNotNull(ref, notNull.message());
	}

	@Override
	public void check(NotBlank notBlank, CharSequence charSequence) {
		checkArgument(checkNotNull(charSequence, notBlank.message()).toString().trim().length() > 0,
				notBlank.message());
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
			// field
			checkArgument(compile(pattern.regexp(), flagValue).matcher(charSequence).matches(), pattern.message());
		}
	}

	@Override
	public void check(Email email, CharSequence charSequence) {
		if (charSequence != null && charSequence.length() != 0) {
			String stringValue = charSequence.toString();
			int splitPosition = stringValue.lastIndexOf('@');
			checkArgument(splitPosition >= 0, email.message());

			String localPart = stringValue.substring(0, splitPosition);
			checkArgument(localPart.length() <= 64 && compile("(?:" + "[a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uFFFF-]"
					+ "+|\"" + "(?:[a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uFFFF-]|\\\\\\\\|\\\\\\\")"
					+ "+\")" + "(?:\\." + "(?:" + "[a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uFFFF-]" + "+|\""
					+ "(?:[a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\u0080-\uFFFF-]|\\\\\\\\|\\\\\\\")" + "+\")" + ")*",
					CASE_INSENSITIVE).matcher(localPart).matches(), email.message());

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
			checkArgument(validEmailDomainAddress, email.message());

			// additional check
			Flag[] flags = email.flags();
			int flagValue = 0;
			for (int i = 0; i < flags.length; i++) {
				jakarta.validation.constraints.Pattern.Flag flag = flags[i];
				flagValue |= flag.getValue();
			}
			checkArgument(compile(email.regexp(), flagValue).matcher(charSequence).matches(), email.message());
		}
	}

	@Override
	public void check(NotEmpty notEmpty, CharSequence charSequence) {
		checkArgument(checkNotNull(charSequence, notEmpty.message()).length() > 0, notEmpty.message());
	}

	public void check(NotEmpty notEmpty, Collection<?> collection) {
		checkArgument(checkNotNull(collection, notEmpty.message()).size() > 0, notEmpty.message());
	}

	@Override
	public void check(NotEmpty notEmpty, Map<?, ?> map) {
		checkArgument(checkNotNull(map, notEmpty.message()).size() > 0, notEmpty.message());
	}

	@Override
	public void check(NotEmpty notEmpty, Object[] objects) {
		checkArgument(checkNotNull(objects, notEmpty.message()).length > 0, notEmpty.message());
	}

	@Override
	public void check(AssertTrue assertTrue, boolean value) {
		checkArgument(value, assertTrue.message());
	}

	@Override
	public void check(AssertTrue assertTrue, Boolean value) {
		checkArgument(value == null || value, assertTrue.message());
	}

	@Override
	public void check(AssertFalse assertFalse, boolean value) {
		checkArgument(!value, assertFalse.message());
	}

	@Override
	public void check(AssertFalse assertFalse, Boolean value) {
		checkArgument(value == null || !value, assertFalse.message());
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Min min, byte value) {
		checkArgument(value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, short value) {
		checkArgument(value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, int value) {
		checkArgument(value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, long value) {
		checkArgument(value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, Byte value) {
		checkArgument(value == null || value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, Short value) {
		checkArgument(value == null || value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, Integer value) {
		checkArgument(value == null || value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, Long value) {
		checkArgument(value == null || value >= min.value(), min.message());
	}

	@Override
	public void check(Min min, BigInteger value) {
		checkArgument(value == null || value.compareTo(BigInteger.valueOf(min.value())) >= 0, min.message());
	}

	@Override
	public void check(Min min, BigDecimal value) {
		checkArgument(value == null || value.compareTo(BigDecimal.valueOf(min.value())) >= 0, min.message());
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Max max, byte value) {
		checkArgument(value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, short value) {
		checkArgument(value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, int value) {
		checkArgument(value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, long value) {
		checkArgument(value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, Byte value) {
		checkArgument(value == null || value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, Short value) {
		checkArgument(value == null || value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, Integer value) {
		checkArgument(value == null || value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, Long value) {
		checkArgument(value == null || value <= max.value(), max.message());
	}

	@Override
	public void check(Max max, BigInteger value) {
		checkArgument(value == null || value.compareTo(BigInteger.valueOf(max.value())) <= 0, max.message());
	}

	@Override
	public void check(Max max, BigDecimal value) {
		checkArgument(value == null || value.compareTo(BigDecimal.valueOf(max.value())) <= 0, max.message());
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Positive positive, byte value) {
		checkArgument(value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, short value) {
		checkArgument(value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, int value) {
		checkArgument(value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, long value) {
		checkArgument(value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, Byte value) {
		checkArgument(value == null || value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, Short value) {
		checkArgument(value == null || value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, Integer value) {
		checkArgument(value == null || value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, Long value) {
		checkArgument(value == null || value > 0, positive.message());
	}

	@Override
	public void check(Positive positive, BigInteger value) {
		checkArgument(value == null || value.signum() > 0, positive.message());
	}

	@Override
	public void check(Positive positive, BigDecimal value) {
		checkArgument(value == null || value.signum() > 0, positive.message());
	}

	// -----------------------------------------------------------------
	@Override
	public void check(PositiveOrZero positiveOrZero, byte value) {
		checkArgument(value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, short value) {
		checkArgument(value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, int value) {
		checkArgument(value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, long value) {
		checkArgument(value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Byte value) {
		checkArgument(value == null || value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Short value) {
		checkArgument(value == null || value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Integer value) {
		checkArgument(value == null || value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, Long value) {
		checkArgument(value == null || value >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, BigInteger value) {
		checkArgument(value == null || value.signum() >= 0, positiveOrZero.message());
	}

	@Override
	public void check(PositiveOrZero positiveOrZero, BigDecimal value) {
		checkArgument(value == null || value.signum() >= 0, positiveOrZero.message());
	}

	// -----------------------------------------------------------------

	@Override
	public void check(Negative negative, byte value) {
		checkArgument(value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, short value) {
		checkArgument(value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, int value) {
		checkArgument(value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, long value) {
		checkArgument(value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, Byte value) {
		checkArgument(value == null || value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, Short value) {
		checkArgument(value == null || value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, Integer value) {
		checkArgument(value == null || value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, Long value) {
		checkArgument(value == null || value < 0, negative.message());
	}

	@Override
	public void check(Negative negative, BigInteger value) {
		checkArgument(value == null || value.signum() < 0, negative.message());
	}

	@Override
	public void check(Negative negative, BigDecimal value) {
		checkArgument(value == null || value.signum() < 0, negative.message());
	}
	// -----------------------------------------------------------------

	@Override
	public void check(NegativeOrZero negativeOrZero, byte value) {
		checkArgument(value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, short value) {
		checkArgument(value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, int value) {
		checkArgument(value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, long value) {
		checkArgument(value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Byte value) {
		checkArgument(value == null || value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Short value) {
		checkArgument(value == null || value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Integer value) {
		checkArgument(value == null || value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, Long value) {
		checkArgument(value == null || value <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, BigInteger value) {
		checkArgument(value == null || value.signum() <= 0, negativeOrZero.message());
	}

	@Override
	public void check(NegativeOrZero negativeOrZero, BigDecimal value) {
		checkArgument(value == null || value.signum() <= 0, negativeOrZero.message());
	}

}
