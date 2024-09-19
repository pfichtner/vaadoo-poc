package com.github.pfichtner.vaadoo;

import java.io.IOException;

import com.github.pfichtner.vaadoo.fragments.GuavaCodeFragment;
import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

public class AddJsr380ValidationPlugin implements Plugin {

	// Read class from some config file (analog lombok.config?)
	private final Class<? extends Jsr380CodeFragment> codeFragment = GuavaCodeFragment.class;

	@Override
	public boolean matches(TypeDescription target) {
		return true;
	}

	@Override
	public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription,
			ClassFileLocator classFileLocator) {
		return builder.visit(new AddValidationToConstructors(codeFragment));
	}

	@Override
	public void close() throws IOException {
		// noop
	}

}