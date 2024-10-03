package com.example;

import static jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE;
import static jakarta.validation.constraints.Pattern.Flag.MULTILINE;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SomeRecord( //
		@Null Object someNullObject, //
		@NotNull Object someObject, //
		String valueWithoutAnnotation, //
		@NotEmpty CharSequence someNotEmptyCharSequence, //
		@NotEmpty String someNotEmptyString, //
		@NotEmpty Collection<String> someNotEmptyCollection, //
		@NotEmpty Map<Integer, String> someNotEmptyMap, //
		@NotEmpty Integer[] someNotEmptyArray, //
		@NotBlank CharSequence someNonBlankValue, //
		@NotBlank(message = "my custom message") String someNonBlankValueWithCustomMessage, //
		@Size(min = 10, max = 20) String stringOfLenfthBetween10And20, //
		@Digits(integer = 4, fraction = 0) int intWith4Digits, //
		@Pattern(regexp = "\\d{1,4}", flags = {
				CASE_INSENSITIVE, MULTILINE }) //
		String someFourDigits, //
		@Email String anyMailAddress, //
		@AssertTrue boolean someTrueValue, //
		@AssertFalse boolean someFalseValue, //
		@AssertTrue Boolean someTrueValueWrapper, //
		@AssertFalse Boolean someFalseValueWrapper, //
		@Min(42) int someIntPrimitiveValueThatIsMinimal42, //
		@Min(42) Long someLongWrapperValueThatIsMinimal42, //
		@DecimalMin(value = "9876543210") long someLongPrimitiveValueWithDecimalMin, //
		@DecimalMin(value = "9876543210") BigDecimal someBigDecimalPrimitiveValueWithDecimalMin, //
		@NotNull @Min(41) @Max(43) Short someShortWrapperValueThatIsNotNullAndAbout42, //
		@NotNull @PastOrPresent Instant somePastOrPresentInstant //
	){

	// provide name clash to test remapping of lambda
	private void lambda$cache$0() {
	}

	public static void main(String[] args) {
		System.out.println(new SomeRecord(null, "notNull", null, " ", " ", List.of("1"), Map.of(1, "a"), new Integer[1],
				"X", "custommessage", "123456789012", 1234, "9999", "me@foo.de", true, false, Boolean.TRUE,
				Boolean.FALSE, 42, Long.valueOf(42), Long.MAX_VALUE, BigDecimal.valueOf(Long.MAX_VALUE),
				Short.valueOf((short) 42), Instant.now()));
	}

}
