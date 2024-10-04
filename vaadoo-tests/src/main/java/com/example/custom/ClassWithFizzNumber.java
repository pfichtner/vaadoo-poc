package com.example.custom;

import lombok.ToString;

@ToString
public class ClassWithFizzNumber {

	// tom make use of the annotation on fields using lombok you'd have to add this
	// to lombok.config (lombok.copyableAnnotations)
	private final Integer number;

	public ClassWithFizzNumber(@FizzBuzzNumber Integer number) {
		this.number = number;
	}

	public static void main(String[] args) {
		System.out.println(new ClassWithFizzNumber(42));
	}

}
