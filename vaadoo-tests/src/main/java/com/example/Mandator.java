package com.example;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Value;

@Value
public class Mandator {

	int id;

	@Getter(value = PRIVATE)
	String cachedStringValue;

	private Mandator(@Min(1) @Max(9999) int id) {
		this.id = id;
		this.cachedStringValue = "Mandator " + format("%1$4s", id).replace(' ', '0');
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
		return cachedStringValue;
	}

}
