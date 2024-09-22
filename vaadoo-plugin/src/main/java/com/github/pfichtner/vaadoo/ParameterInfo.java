package com.github.pfichtner.vaadoo;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.bytebuddy.jar.asm.Type;

class ParameterInfo {

	static class EnumEntry {
		Type type;
		String value;

		EnumEntry(Type type, String value) {
			this.type = type;
			this.value = value;
		}

		Type type() {
			return type;
		}

		String value() {
			return value;
		}
	}

	private final int index;
	private String name;
	private Type type;
	private final Map<Type, Map<String, Object>> annotationValues = new LinkedHashMap<>();

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

	public Class<?> classtype() {
		return AsmUtil.classtype(type);
	}

	public boolean isArray() {
		return AsmUtil.isArray(type);
	}

	public String classname() {
		return type.getClassName();
	}

	public void addAnnotation(Type annotation) {
		annotationValues.put(annotation, new HashMap<>());
	}

	public Collection<Type> getAnnotations() {
		return annotationValues.keySet();
	}

	public void addAnnotationValue(Type descriptor, String key, Object value) {
		annotationValues.computeIfAbsent(descriptor, k -> new HashMap<>()).put(key, value);
	}

	public Object annotationValue(Type descriptor, String name) {
		return annotationValues.getOrDefault(descriptor, emptyMap()).get(name);
	}

	@Override
	public String toString() {
		return "ParameterInfo [index=" + index + ", name=" + name + ", type=" + type + ", annotationValues="
				+ annotationValues + "]";
	}

}