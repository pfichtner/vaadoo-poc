package com.example.custom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ClassWithFizzNumberTest {

	@Test
	void failsIfNumberIsNotOneOfFizzBuzz() {
		assertThatThrownBy(() -> new ClassWithFizzNumber(4)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("number" + " not valid");
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

	@Test
	void customeMessage() {
		assertThatThrownBy(() -> new ClassWithFizzNumber(4, true)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("other message");
	}

}
