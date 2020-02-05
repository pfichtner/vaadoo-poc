package com.github.pfichtner.bytebuddy.pg._1_manual;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;

class TestPerson {

	@Test
	void assertThrowsException() {
		assertThrows(ConstraintViolationException.class, () -> Person.create(null));
		assertThrows(ConstraintViolationException.class, () -> Person.create("x"));
	}

	@Test
	void assertok() {
		Person.create("xx");
	}

}
