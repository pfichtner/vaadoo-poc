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
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class Mandator {

	private static final String RANGE_ERROR = "Mandator muss zwischen 1 und 9999 liegen";
	private static final String NOT_NUMERIC = "Mandator muss numerisch sein";

	@Min(value = 1, message = RANGE_ERROR)
	@Max(value = 9999, message = RANGE_ERROR)
	int id;

	@Getter(value = PRIVATE)
	String cachedStringValue;

	private Mandator(int id) {
		this(id, "Mandator " + format("%1$4s", id).replace(' ', '0'));
	}

	private Mandator(@AssertTrue boolean foo, @NotEmpty @Pattern(regexp = "\\d+", message = NOT_NUMERIC) String id,
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
