package com.github.pfichtner.vaadoo.supplier;

import java.util.Arrays;

public class Example {

	private final Class<Object> type;
	private final Object example;

	public static Object nullValue() {
		return null;
	}

	@SuppressWarnings("unchecked")
	public Example(Class<?> type, Object example) {
		this.type = (Class<Object>) type;
		this.example = example;
	}

	public Class<Object> type() {
		return type;
	}

	public Object value() {
		return example;
	}

	@Override
	public String toString() {
		Object value = valueString();
		return "Example [type=" + type.getName() + ", example=" + value + "]";
	}

	private Object valueString() {
		return example == null //
				? null //
				: example.getClass().isArray() //
						? Arrays.toString((Object[]) example) //
						: example;
	}

}
