package com.github.pfichtner.vaadoo;

import static net.bytebuddy.jar.asm.Type.ARRAY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.bytebuddy.jar.asm.Type;

class ParameterInfo {

	private final int index;
	private String name;
	private Type type;
	private final List<String> annotations = new ArrayList<>();
	private final Map<String, Object> annotationValues = new HashMap<String, Object>();

	public ParameterInfo(int index) {
		this.index = index;
	}

	public ParameterInfo name(String name) {
		this.name = name;
		return this;
	}

	public int index() {
		return index;
	}

	public String name() {
		return name;
	}

	public ParameterInfo type(Type type) {
		this.type = type;
		return this;
	}

	public Type type() {
		return type;
	}

	public boolean typeIs(Class<?> clazz) {
		return typeIs(Type.getType(clazz));
	}

	public boolean typeIs(Type other) {
		return type().equals(other);
	}

	public boolean isArray() {
		return type.getSort() == ARRAY;
	}

	public String classname() {
		return type.getClassName();
	}

	public void addAnnotation(String descriptor) {
		annotations.add(descriptor);
	}

	public boolean hasAnnotation(String descriptor) {
		return annotations.contains(descriptor);
	}

	public void addAnnotationValue(String key, Object value) {
		annotationValues.put(key, value);
	}

	public Optional<Object> annotationValue(String key) {
		return Optional.ofNullable(annotationValues.get(key));
	}

	@Override
	public String toString() {
		return "ParameterInfo [index=" + index + ", name=" + name + ", type=" + type + ", annotations=" + annotations
				+ ", annotationValues=" + annotationValues + "]";
	}

}