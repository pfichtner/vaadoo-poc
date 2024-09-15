package com.example;

import java.util.Collection;
import java.util.Map;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Builder;

@Builder
//@FieldDefaults(level = AccessLevel.PRIVATE)
public class SomeLombokClass {

	@Null
	Object someNullObject;
	@NotNull
	private final Object someObject;
	private final String valueWithoutAnnotation;
	@NotEmpty
	private final CharSequence someNotEmptyCharSequence;
	@NotEmpty
	private final String someNotEmptyString;
	@NotEmpty
	private final Collection<String> someNotEmptyCollection;
	@NotEmpty
	private final Map<Integer, String> someNotEmptyMap;
	@NotEmpty
	private final Integer[] someNotEmptyArray;
	@NotBlank
	private final CharSequence someNonBlankValue;
	@AssertTrue
	private final boolean someTrueValue;
	@AssertFalse
	private final boolean someFalseValue;
	@AssertTrue
	private final Boolean someTrueValueWrapper;
	@AssertFalse
	private final Boolean someFalseValueWrapper;
	@Min(42)
	private final int someValueThatIsMinimal42;

	public static void main(String[] args) {
		System.out.println(SomeLombokClass.builder().someObject("isNotNull").someNonBlankValue("").build());
	}

}
