package com.github.pfichtner.vaadoo.fragments;

import java.lang.annotation.Annotation;

import jakarta.validation.ConstraintValidator;

public interface Jsr380CodeCustomFragment {

	void check(Annotation anno, Object value, ConstraintValidator<?, Object> validator);

}
