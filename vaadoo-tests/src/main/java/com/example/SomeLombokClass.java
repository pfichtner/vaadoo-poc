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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
	Short someShortWrapperValueThatIsNotNullAndMinimal42;

	public static void main(String[] args) {
		System.out.println(SomeLombokClass.builder().someObject("isNotNull").someNonBlankValue("").build());
	}

}
