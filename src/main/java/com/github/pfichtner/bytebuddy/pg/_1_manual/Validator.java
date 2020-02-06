package com.github.pfichtner.bytebuddy.pg._1_manual;

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Validator {

	public static <T> T validate(T t) {
		Set<ConstraintViolation<T>> validationResult = Validation.buildDefaultValidatorFactory().getValidator()
				.validate(t);
		if (!validationResult.isEmpty()) {
			throw new ConstraintViolationException(validationResult);
		}
		return t;
	}

}
