package com.github.pfichtner.vaadoo.supplier;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ArbitrarySupplier;

public class CharSequenceClasses implements ArbitrarySupplier<Class<?>> {

	@Override
	public Arbitrary<Class<?>> get() {
		return Arbitraries.of(CharSequence.class, String.class);
	}

}
