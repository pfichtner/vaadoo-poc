package com.example;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class Mandator {

	int id;

	String stringValue;

	private Mandator(@Min(1) @Max(9999) int id) {
		this.id = id;
		this.stringValue = format("%1$4s", id).replace(' ', '0');
	}

	private Mandator(@AssertTrue boolean foo,
			@NotEmpty @Pattern(regexp = "\\d+", message = "Mandator muss numerisch sein") String id,
			@AssertFalse boolean bar) {
		this(parseInt(id));
	}

	public Mandator(@NotEmpty String[] args) {
		this(true, args[0], false);
	}

	public static void main(String[] args) {
		System.out.println(new Mandator(args));
	}

	@Override
	public String toString() {
		return "Mandator " + stringValue;
	}

}
