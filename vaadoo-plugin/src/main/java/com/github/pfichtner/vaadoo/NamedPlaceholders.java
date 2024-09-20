package com.github.pfichtner.vaadoo;

import static java.util.regex.Matcher.quoteReplacement;
import static java.util.regex.Pattern.compile;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NamedPlaceholders {

	private NamedPlaceholders() {
		super();
	}

	public static String replace(String template, Map<String, Object> replacements) {
		return replace(template, k -> replacements.getOrDefault(k, k));
	}

	public static String replace(String template, Function<String, Object> resolver) {
		Pattern pattern = compile("\\{([^}]+)\\}");
		String result = template;
		while (true) {
			Matcher matcher = pattern.matcher(result);
			StringBuilder sb = new StringBuilder();
			boolean replaced = false;
			while (matcher.find()) {
				String key = matcher.group(1);
				Object replacement = resolver.apply(key);
				if (!Objects.equals(replacement, key)) {
					replaced = true;
					matcher.appendReplacement(sb, quoteReplacement(String.valueOf(replacement)));
				}
			}
			matcher.appendTail(sb);
			if (!replaced) {
				break;
			}
			result = sb.toString();
		}
		return result;
	}

}
