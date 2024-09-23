package com.example;

import static jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE;
import static jakarta.validation.constraints.Pattern.Flag.MULTILINE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SomeClass {

	private final Object someNullObject;
	private final Object someObject;
	private final String valueWithoutAnnotation;
	private final CharSequence someNotEmptyCharSequence;
	private final String someNotEmptyString;
	private final Collection<String> someNotEmptyCollection;
	private final Map<Integer, String> someNotEmptyMap;
	private final Integer[] someNotEmptyArray;
	private final CharSequence someNonBlankValue;
	private final String stringOfLenfthBetween10And20;
	private final String someNonBlankValueWithCustomMessage;
	private final String someFourDigits;
	private final String anyMailAddress;
	private final boolean someTrueValue;
	private final boolean someFalseValue;
	private final Boolean someTrueValueWrapper;
	private final Boolean someFalseValueWrapper;
	private final int someIntPrimitiveValueThatIsMinimal42;
	private final Long someLongWrapperValueThatIsMinimal42;
	private final Short someShortWrapperValueThatIsNotNullAndAbout42;

	public SomeClass( //
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
			@Size(min = 10, max = 20) String stringOfLenfthBetween10And20,
			@Pattern(regexp = "\\d{1,4}", flags = { CASE_INSENSITIVE, MULTILINE }) String someFourDigits, //
			@Email String anyMailAddress, //
			@AssertTrue boolean someTrueValue, //
			@AssertFalse boolean someFalseValue, //
			@AssertTrue Boolean someTrueValueWrapper, //
			@AssertFalse Boolean someFalseValueWrapper, //
			@Min(42) int someIntPrimitiveValueThatIsMinimal42, //
			@Min(42) Long someLongWrapperValueThatIsMinimal42, //
			@NotNull @Min(41) @Max(43) Short someShortWrapperValueThatIsNotNullAndAbout42
	//
	) {
		this.someNullObject = someNullObject;
		this.someObject = someObject;
		this.valueWithoutAnnotation = valueWithoutAnnotation;
		this.someNotEmptyCharSequence = someNotEmptyCharSequence;
		this.someNotEmptyString = someNotEmptyString;
		this.someNotEmptyCollection = someNotEmptyCollection;
		this.someNotEmptyMap = someNotEmptyMap;
		this.someNotEmptyArray = someNotEmptyArray;
		this.someNonBlankValue = someNonBlankValue;
		this.stringOfLenfthBetween10And20 = stringOfLenfthBetween10And20;
		this.someNonBlankValueWithCustomMessage = someNonBlankValueWithCustomMessage;
		this.someFourDigits = someFourDigits;
		this.anyMailAddress = anyMailAddress;
		this.someTrueValue = someTrueValue;
		this.someFalseValue = someFalseValue;
		this.someTrueValueWrapper = someTrueValueWrapper;
		this.someFalseValueWrapper = someFalseValueWrapper;
		this.someIntPrimitiveValueThatIsMinimal42 = someIntPrimitiveValueThatIsMinimal42;
		this.someLongWrapperValueThatIsMinimal42 = someLongWrapperValueThatIsMinimal42;
		this.someShortWrapperValueThatIsNotNullAndAbout42 = someShortWrapperValueThatIsNotNullAndAbout42;
	}

	public static void main(String[] args) {
		System.out.println(new SomeClass(null, "isNotNull", null, null, null, List.of(), Map.of(), new Integer[0], "",
				"", "", "1234", "me@example.com", true, false, Boolean.TRUE, Boolean.FALSE, 42, Long.valueOf(42),
				Short.valueOf((short) 42)));
	}

}
