package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.NamedPlaceholders.replace;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Map;

import org.junit.jupiter.api.Test;

class NamedPlaceholdersTest {

	@Test
	void noReplacment() {
		String in = "in";
		assertThat(replace(in, emptyMap())).isEqualTo(in);
	}

	@Test
	void simpleReplacmentMatch() {
		String in = "prefix {theKey} sufix";
		String expected = "prefix aValue sufix";
		assertThat(replace(in, Map.of("theKey", "aValue"))).isEqualTo(expected);
	}

	@Test
	void simpleReplacmentNoMatch() {
		String in = "prefix {thekey} sufix";
		assertThat(replace(in, emptyMap())).isEqualTo(in);
	}

	@Test
	void evalTrue() {
		String in = "prefix ${aBooleanValue == true ? 'someString' : 'anotherString'} sufix";
		String expected = "prefix someString sufix";
		assertThat(replace(in, Map.of("aBooleanValue", true))).isEqualTo(expected);
	}

	@Test
	void evalFalse() {
		String in = "prefix ${aBooleanValue == true ? 'someString' : 'anotherString'} sufix";
		String expected = "prefix anotherString sufix";
		assertSoftly(s -> {
			s.assertThat(replace(in, Map.of("aBooleanValue", false))).isEqualTo(expected);
			s.assertThat(replace(in, emptyMap())).isEqualTo(expected);
		});
	}

}
