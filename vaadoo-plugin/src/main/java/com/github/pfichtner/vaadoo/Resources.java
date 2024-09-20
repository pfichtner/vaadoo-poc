package com.github.pfichtner.vaadoo;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.ResourceBundle;

public final class Resources {

	private static final Map<String, String> messages = loadMessages();

	private Resources() {
		super();
	}

	public static String message(String key) {
		return messages.getOrDefault(key, key);
	}

	private static Map<String, String> loadMessages() {
		ResourceBundle bundle = ResourceBundle.getBundle("com/github/pfichtner/vaadoo");
		return bundle.keySet().stream().collect(toMap(key -> key, bundle::getString));
	}

}
