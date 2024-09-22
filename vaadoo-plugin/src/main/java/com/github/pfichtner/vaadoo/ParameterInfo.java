package com.github.pfichtner.vaadoo;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.bytebuddy.jar.asm.Type;

class ParameterInfo {

	private final int index;
	private String name;
	private Type type;
	private final Map<Type, Map<String, Object>> annotationValues = new LinkedHashMap<>();

	// TODO there values are per annotation as well!
	private final Map<String, Map<Type, String>> arrayValues = new HashMap<>();
	private final Map<String, Map<Type, String>> arrayValues_ = unmodifiableMap(arrayValues);

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

	public Map<Type, String> addAnnotationArray(String name) {
		Map<Type, String> elements;
		this.arrayValues.put(name, elements = new HashMap<>());
		return elements;
	}

	public void addAnnotationArrayElement(String array, Type type, String value) {
		arrayValues.computeIfAbsent(array, k -> new HashMap<>()).put(type, value);
	}

	public Map<String, Map<Type, String>> arrayValues() {
		return arrayValues_;
	}

	@Override
	public String toString() {
		return "ParameterInfo [index=" + index + ", name=" + name + ", type=" + type + ", annotationValues="
				+ annotationValues + ", arrayValues=" + arrayValues + "]";
	}

}