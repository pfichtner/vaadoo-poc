package com.example.custom;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ClassWithFizzNumberTest {

	private static final int NOT_FIZZ_NOR_BUZZ = 1;

	@Test
	void failsIfNumberIsNotOneOfFizzBuzz() {
		assertThatThrownBy(() -> new ClassWithFizzNumber(NOT_FIZZ_NOR_BUZZ)) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessage("number must be divisible by 3 or 5");
	}

	@Test
	void customeMessage() {
		assertThatThrownBy(() -> new ClassWithFizzNumber(4, true)) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessage("other message");
	}

	@Test
	void okIfNumberIsOneOfFizzBuzz() {
		assertDoesNotThrow(() -> {
			new ClassWithFizzNumber(3);
			new ClassWithFizzNumber(5);
			new ClassWithFizzNumber(6);
			new ClassWithFizzNumber(15);
		});
	}

}
