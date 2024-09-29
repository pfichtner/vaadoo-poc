package com.github.pfichtner.vaadoo.fragments.impl;

import static java.util.regex.Pattern.compile;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class RegexWithFlagsPatternCache {

	private static Map<SimpleEntry<String, Integer>, Pattern> cache;

	private static synchronized Pattern cache(String regex, int flags) {
		// this is a workaround to prevent merging of clinits
		if (cache == null) {
			cache = new HashMap<>();
		}
		return cache.computeIfAbsent(new SimpleEntry<>(regex, flags), e -> compile(e.getKey(), e.getValue()));
	}

}
