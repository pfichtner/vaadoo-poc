package com.github.pfichtner.vaadoo.fragments.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class RegexPatternCache {

	private static final Map<String, Pattern> cache = new ConcurrentHashMap<>();

	private static Pattern cachedRegex(String regex) {
		return cache.computeIfAbsent(regex, Pattern::compile);
	}

}
