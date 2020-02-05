package com.github.pfichtner.bytebuddy.pg._1_manual;

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.github.pfichtner.bytebuddy.pg._2_own_anno_with_agent.Validate;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
@Validate
public class Person {

	@NotNull
	@Size(min = 2, max = 20)
	private final String name;

	public static Person create(String name) {
		return validate(new Person(name));
	}

	private static <T> T validate(T t) {
		Set<ConstraintViolation<T>> validationResult = Validation.buildDefaultValidatorFactory().getValidator()
				.validate(t);
		if (!validationResult.isEmpty()) {
			throw new ConstraintViolationException(validationResult);
		}
		return t;
	}

}
