package com.github.pfichtner.vaadoo.supplier;

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

}
