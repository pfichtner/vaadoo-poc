package com.github.pfichtner.vaadoo;

import static java.lang.reflect.Array.newInstance;
import static java.util.Collections.unmodifiableMap;
import static net.bytebuddy.jar.asm.Type.ARRAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import net.bytebuddy.jar.asm.Type;

class ParameterInfo {

	private final int index;
	private String name;
	private Type type;
	private final List<String> annotations = new ArrayList<>();
	private final Map<String, Object> annotationValues = new HashMap<String, Object>();
	private final Map<String, Object> annotationValues_ = unmodifiableMap(annotationValues);

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
		try {
			switch (type.getSort()) {
			case Type.BOOLEAN:
				return boolean.class;
			case Type.CHAR:
				return char.class;
			case Type.BYTE:
				return byte.class;
			case Type.SHORT:
				return short.class;
			case Type.INT:
				return int.class;
			case Type.FLOAT:
				return float.class;
			case Type.LONG:
				return long.class;
			case Type.DOUBLE:
				return double.class;
			case Type.VOID:
				return void.class;
			case Type.ARRAY:
				var elementType = type.getElementType();
				return newInstance(Class.forName(elementType.getClassName()), elementType.getDimensions()).getClass();
			case Type.OBJECT:
				return Class.forName(type.getClassName());
			default:
				throw new IllegalArgumentException("Unknown type: " + type);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
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

	public List<String> getAnnotations() {
		return annotations;
	}

	public void addAnnotationValue(String key, Object value) {
		annotationValues.put(key, value);
	}

	public Optional<Object> annotationValue(String key) {
		return Optional.ofNullable(annotationValues.get(key));
	}

	public Map<String, Object> annotationValues() {
		return annotationValues_;
	}

	@Override
	public String toString() {
		return "ParameterInfo [index=" + index + ", name=" + name + ", type=" + type + ", annotations=" + annotations
				+ ", annotationValues=" + annotationValues + "]";
	}

}