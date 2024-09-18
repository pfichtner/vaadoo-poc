package com.github.pfichtner.vaadoo.supplier;

public class TypeAndExample {

	private final Class<Object> type;
	private final Object example;

	@SuppressWarnings("unchecked")
	public TypeAndExample(Class<?> type, Object example) {
		this.type = (Class<Object>) type;
		this.example = example;
	}

	public Class<Object> type() {
		return type;
	}

	public Object example() {
		return example;
	}

}
