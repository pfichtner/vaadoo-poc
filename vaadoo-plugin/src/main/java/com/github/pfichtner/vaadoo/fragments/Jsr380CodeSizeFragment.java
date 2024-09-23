package com.github.pfichtner.vaadoo.fragments;

import java.util.Collection;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public interface Jsr380CodeSizeFragment {

	void check(NotEmpty notEmpty, Collection<?> collection);

	void check(NotEmpty notEmpty, Map<?, ?> map);

	void check(NotEmpty notEmpty, Object[] objects);

}
