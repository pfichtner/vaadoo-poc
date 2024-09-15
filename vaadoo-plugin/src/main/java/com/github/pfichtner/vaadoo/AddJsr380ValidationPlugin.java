package com.github.pfichtner.vaadoo;

import java.io.IOException;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

public class AddJsr380ValidationPlugin implements Plugin {

	// Read class from some config file (analog lombok.config?)
	private final CodeEmitter codeEmitter = new GuavaCodeEmitter();

	@Override
	public boolean matches(TypeDescription target) {
		return true;
	}

	@Override
	public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription,
			ClassFileLocator classFileLocator) {
		return builder.visit(new AddValidationToConstructors(codeEmitter));
	}

	@Override
	public void close() throws IOException {
		// noop
	}

}