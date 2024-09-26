package com.github.pfichtner.vaadoo.fragments.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class RegexPatternCache {

	private static final Map<String, Pattern> cachedRegex = new ConcurrentHashMap<>();

	private static Pattern cachedRegex(String regex) {
		return cachedRegex.computeIfAbsent(regex, Pattern::compile);
	}

}
