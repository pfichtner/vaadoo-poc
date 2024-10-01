package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.NamedPlaceholders.replace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Map;

import org.junit.jupiter.api.Test;

class NamedPlaceholdersTest {

	@Test
	void noReplacment() {
		String in = "in";
		assertThat(replace(in, Map.of())).isEqualTo(in);
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
		assertThat(replace(in, Map.of())).isEqualTo(in);
	}

	@Test
	void evalTrue() {
		String in = "prefix ${inclusive == true ? 'someString' : 'anotherString'} sufix";
		String expected = "prefix someString sufix";
		assertThat(replace(in, Map.of("inclusive", true))).isEqualTo(expected);
	}

	@Test
	void evalFalse() {
		String in = "prefix ${inclusive == true ? 'someString' : 'anotherString'} sufix";
		String expected = "prefix anotherString sufix";
		assertSoftly(s -> {
			s.assertThat(replace(in, Map.of("inclusive", false))).isEqualTo(expected);
			s.assertThat(replace(in, Map.of())).isEqualTo(expected);
		});
	}

}
