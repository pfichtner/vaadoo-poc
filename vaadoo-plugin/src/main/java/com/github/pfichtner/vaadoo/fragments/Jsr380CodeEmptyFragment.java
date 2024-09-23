package com.github.pfichtner.vaadoo.fragments;

import java.util.Collection;
import java.util.Map;

import jakarta.validation.constraints.Size;

public interface Jsr380CodeEmptyFragment {

	void check(Size size, CharSequence collection);

	void check(Size size, Collection<?> collection);

	void check(Size size, Map<?, ?> map);

	void check(Size size, Object[] objects);

}
