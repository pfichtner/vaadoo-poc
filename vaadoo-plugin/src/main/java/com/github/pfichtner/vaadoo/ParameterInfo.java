package com.github.pfichtner.vaadoo;

import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.Collections;
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
	private final Map<String, Object> annotationValues_ = unmodifiableMap(annotationValues);

	private final Map<String, Map<Type, String>> arrayValues = new HashMap<>();
	private final Map<String, Map<Type, String>> arrayValues_ = Collections.unmodifiableMap(arrayValues);

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

	public void addAnnotation(String descriptor) {
		annotations.add(descriptor);
	}

	public List<String> getAnnotations() {
		return annotations;
	}

	public void addAnnotationValue(String key, Object value) {
		annotationValues.put(key, value);
	}

	public Optional<Object> annotationValue(String key) {
		return Optional.ofNullable(annotationValues.get(key));
	}

	public Map<Type, String> addAnnotationArray(String name) {
		Map<Type, String> elements;
		this.arrayValues.put(name, elements = new HashMap<>());
		return elements;
	}

	public Map<String, Object> annotationValues() {
		return annotationValues_;
	}

	public void addAnnotationArrayElement(String array, Type type, String value) {
		arrayValues.computeIfAbsent(array, k -> new HashMap<>()).put(type, value);
	}

	public Map<String, Map<Type, String>> arrayValues() {
		return arrayValues_;
	}

	@Override
	public String toString() {
		return "ParameterInfo [index=" + index + ", name=" + name + ", type=" + type + ", annotations=" + annotations
				+ ", annotationValues=" + annotationValues + ", arrayValues=" + arrayValues + "]";
	}

}