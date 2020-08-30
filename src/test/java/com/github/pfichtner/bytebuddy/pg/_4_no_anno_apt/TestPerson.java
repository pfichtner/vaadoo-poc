package com.github.pfichtner.bytebuddy.pg._4_no_anno_apt;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;

class TestPerson {

	@Test
	void assertThrowsException() {
		assertThrows(ConstraintViolationException.class, () -> new Person(null));
		assertThrows(ConstraintViolationException.class, () -> new Person("x"));
	}

	@Test
	void assertOk() {
		new Person("xxx");
	}

}
