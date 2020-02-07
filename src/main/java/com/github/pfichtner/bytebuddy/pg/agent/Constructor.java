package com.github.pfichtner.bytebuddy.pg.agent;

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;

import lombok.NoArgsConstructor;
import net.bytebuddy.asm.Advice;

@NoArgsConstructor(access = PRIVATE)
public final class Constructor {

	@Advice.OnMethodExit
	public static void exitConstructor(@Advice.This Object thisObj) {
		Set<ConstraintViolation<Object>> validationResult = Validation.buildDefaultValidatorFactory().getValidator()
				.validate(thisObj);
		if (!validationResult.isEmpty()) {
			throw new ConstraintViolationException(validationResult);
		}
	}

}