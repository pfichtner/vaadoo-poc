package com.github.pfichtner.vaadoo;

import static java.lang.String.format;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NamedPlaceholders {

	private NamedPlaceholders() {
		super();
	}

	public static String quote(String string) {
		return format("{%s}", string);
	}

	public static String replace(String template, Map<String, Object> replacements) {
		Pattern pattern = Pattern.compile("(\\{[^}]+\\})");
		String result = template;
		while (true) {
			Matcher matcher = pattern.matcher(result);
			StringBuffer sb = new StringBuffer();
			boolean replaced = false;
			while (matcher.find()) {
				String key = matcher.group(1);
				String replacement = replacements.getOrDefault(key, key).toString();
				if (!replacement.equals(key)) {
					replaced = true;
					matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
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
