package com.example;

import static jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE;
import static jakarta.validation.constraints.Pattern.Flag.MULTILINE;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
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
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SomeLombokClass {

	@Null
	Object someNullObject;
	@NotNull
	Object someObject;
	String valueWithoutAnnotation;
	@NotEmpty
	CharSequence someNotEmptyCharSequence;
	@NotEmpty
	String someNotEmptyString;
	@NotEmpty
	Collection<String> someNotEmptyCollection;
	@NotEmpty
	Map<Integer, String> someNotEmptyMap;
	@NotEmpty
	Integer[] someNotEmptyArray;
	@NotBlank
	CharSequence someNonBlankValue;
	@NotBlank(message = "my custom message")
	String someNonBlankValueWithCustomMessage;
	@Pattern(regexp = "\\d{1,4}", flags = { CASE_INSENSITIVE, MULTILINE })
	String someFourDigits;
	@Email
	String anyMailAddress;
	@AssertTrue
	boolean someTrueValue;
	@AssertFalse
	boolean someFalseValue;
	@AssertTrue
	Boolean someTrueValueWrapper;
	@AssertFalse
	Boolean someFalseValueWrapper;
	@Min(42)
	int someIntPrimitiveValueThatIsMinimal42;
	@Min(42)
	Long someLongWrapperValueThatIsMinimal42;
	@NotNull
	@Min(42)
	@Max(42)
	Short someShortWrapperValueThatIsNotNullAndMinimal42;

	public static void main(String[] args) {
		System.out.println(SomeLombokClass.builder().someObject("isNotNull").someNonBlankValue("").build());
	}

}
