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
		@AssertTrue boolean someTrueValue, //
		@AssertFalse boolean someFalseValue, //
		@AssertTrue Boolean someTrueValueWrapper, //
		@AssertFalse Boolean someFalseValueWrapper, //
		@Min(42) int someIntPrimitiveValueThatIsMinimal42, //
		@Min(42) Long someLongWrapperValueThatIsMinimal42) {

	public static void main(String[] args) {
		System.out.println(new SomeRecord(null, "isNotNull", " ", " ", " ", List.of(" "), Map.of(0, " "),
				new Integer[] { 1 }, "", true, false, Boolean.TRUE, Boolean.FALSE, 42, 42L));
	}

}
