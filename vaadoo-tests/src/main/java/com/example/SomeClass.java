package com.example;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

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
	private final boolean someTrueValue;
	private final boolean someFalseValue;
	private final Boolean someTrueValueWrapper;
	private final Boolean someFalseValueWrapper;
	private final int someValueThatIsMinimal42;

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
			@AssertTrue boolean someTrueValue, //
			@AssertFalse boolean someFalseValue, //
			@AssertTrue Boolean someTrueValueWrapper, //
			@AssertFalse Boolean someFalseValueWrapper, //
			@Min(42) int someValueThatIsMinimal42) {
		this.someNullObject = someNullObject;
		this.someObject = someObject;
		this.valueWithoutAnnotation = valueWithoutAnnotation;
		this.someNotEmptyCharSequence = someNotEmptyCharSequence;
		this.someNotEmptyString = someNotEmptyString;
		this.someNotEmptyCollection = someNotEmptyCollection;
		this.someNotEmptyMap = someNotEmptyMap;
		this.someNotEmptyArray = someNotEmptyArray;
		this.someNonBlankValue = someNonBlankValue;
		this.someTrueValue = someTrueValue;
		this.someFalseValue = someFalseValue;
		this.someTrueValueWrapper = someTrueValueWrapper;
		this.someFalseValueWrapper = someFalseValueWrapper;
		this.someValueThatIsMinimal42 = someValueThatIsMinimal42;
	}

	public static void main(String[] args) {
		System.out.println(new SomeClass(null, "isNotNull", null, null, null, List.of(), Map.of(), new Integer[0], "",
				true, false, Boolean.TRUE, Boolean.FALSE, 42));
	}

}
