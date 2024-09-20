package com.github.pfichtner.vaadoo;

import static java.lang.String.format;
import static java.util.regex.Matcher.quoteReplacement;
import static java.util.regex.Pattern.compile;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NamedPlaceholders {

	private NamedPlaceholders() {
		super();
	}

	public static String quote(String string) {
		return format("{%s}", string);
	}

	public static String unquote(String string) {
		Pattern pattern = compile("\\{([^}]+)\\}");
		Matcher matcher = pattern.matcher(string);
		return matcher.find() ? matcher.group(1) : string;
	}

	public static String replace(String template, Map<String, Object> replacements) {
		return replace(template, k -> replacements.getOrDefault(k, k));
	}

	public static String replace(String template, Function<String, Object> replacements) {
		Pattern pattern = compile("(\\{[^}]+\\})");
		String result = template;
		while (true) {
			Matcher matcher = pattern.matcher(result);
			StringBuffer sb = new StringBuffer();
			boolean replaced = false;
			while (matcher.find()) {
				String key = matcher.group(1);
				String replacement = replacements.apply(key).toString();
				if (!replacement.equals(key)) {
					replaced = true;
					matcher.appendReplacement(sb, quoteReplacement(replacement));
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
