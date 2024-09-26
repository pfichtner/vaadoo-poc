package com.github.pfichtner.vaadoo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClassMembers {

	private final List<String> fieldNames = new ArrayList<>();
	private final List<String> methodNames = new ArrayList<>();

	public void addFieldNames(Stream<String> toAdd) {
		toAdd.forEach(fieldNames::add);
	}

	public boolean containsFieldName(String fieldName) {
		return fieldNames.contains(fieldName);
	}

	public void addMethodNames(Stream<String> toAdd) {
		toAdd.forEach(methodNames::add);
	}

	public String newMethod(String arg) {
		String newMethodName = arg;
		for (int i = 1; methodNames.contains(newMethodName); i++) {
			newMethodName = arg + "$" + i;
		}
		return newMethodName;
	}

}
