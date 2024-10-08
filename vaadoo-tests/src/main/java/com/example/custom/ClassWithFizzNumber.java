package com.example.custom;

import lombok.ToString;

@ToString
public class ClassWithFizzNumber {

	// to make use of the annotation on fields using lombok you'd have to add
	// @FizzBuzzNumber to lombok.config (lombok.copyableAnnotations)
	// @FizzBuzzNumber
	private final Integer number;

	public ClassWithFizzNumber(@FizzBuzzNumber Integer number) {
		this.number = number;
	}

	public ClassWithFizzNumber(@FizzBuzzNumber(message = "other message") Integer number, boolean ___) {
		this.number = number;
	}

	public static void main(String[] args) {
		System.out.println(new ClassWithFizzNumber(42));
	}

}
