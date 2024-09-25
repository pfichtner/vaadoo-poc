package com.github.pfichtner.vaadoo.fragments.impl;

import static java.util.regex.Pattern.compile;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class RegexWithFlagsPatternCache {

	private static final Map<AbstractMap.SimpleEntry<String, Integer>, Pattern> cachedRegexWithFlags = new ConcurrentHashMap<>();

	private static Pattern cachedRegex(String regex, int flags) {
		return cachedRegexWithFlags.computeIfAbsent( //
				new AbstractMap.SimpleEntry<>(regex, flags), //
				e -> compile(e.getKey(), e.getValue()));
	}

}
