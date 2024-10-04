package com.example.custom;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FizzBuzzNumberValidator implements ConstraintValidator<FizzBuzzNumber, Integer> {

	@Override
	public boolean isValid(Integer value, ConstraintValidatorContext context) {
		return value == null || isFizz(value) || isBuzz(value);
	}

	private static boolean isFizz(Integer value) {
		return value % 3 == 0;
	}

	private static boolean isBuzz(Integer value) {
		return value % 5 == 0;
	}

}
