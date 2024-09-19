package com.github.pfichtner.vaadoo.fragments;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;

public interface Jsr380CodeBooleanFragment {

	void check(AssertTrue assertTrue, boolean value);

	void check(AssertTrue assertTrue, Boolean value);

	void check(AssertFalse assertFalse, boolean value);

	void check(AssertFalse assertFalse, Boolean value);

}
