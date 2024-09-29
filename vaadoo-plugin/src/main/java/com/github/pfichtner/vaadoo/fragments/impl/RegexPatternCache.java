package com.github.pfichtner.vaadoo.fragments.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class RegexPatternCache {

	private static Map<String, Pattern> cache;

	private static synchronized Pattern cache(String regex) {
		// this is a workaround to prevent merging of clinits 
		if (cache == null) {
			cache = new HashMap<>();
		}
		return cache.computeIfAbsent(regex, Pattern::compile);
	}

}
