package com.github.pfichtner.vaadoo;

import static net.bytebuddy.jar.asm.ClassWriter.COMPUTE_FRAMES;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.github.pfichtner.vaadoo.fragments.Jsr380CodeFragment;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldDescription.InDefinedShape;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.pool.TypePool;

public class AddValidationToConstructors implements AsmVisitorWrapper {

	private final Class<? extends Jsr380CodeFragment> codeFragment;
	private final List<Method> codeFragmentMethods;

	public AddValidationToConstructors(Class<? extends Jsr380CodeFragment> codeFragment) {
		this.codeFragment = codeFragment;
		this.codeFragmentMethods = Arrays.asList(codeFragment.getMethods());
	}

	@Override
	public int mergeReader(int flags) {
		return flags;
	}

	@Override
	public int mergeWriter(int flags) {
		return flags | COMPUTE_FRAMES;
	}

	@Override
	public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Context implementationContext,
			TypePool typePool, FieldList<InDefinedShape> fields, MethodList<?> methods, int writerFlags,
			int readerFlags) {
		ClassMembers classMembers = new ClassMembers();
		classMembers.addMethodNames(methods.stream().map(MethodDescription::getName));
		classMembers.addFieldNames(fields.stream().map(FieldDescription::getName));

		// TODO make configurable
		boolean optimizeRegex = true;
		ClassVisitor classVisitor2 = optimizeRegex ? new CacheRegexCompileCalls(classVisitor, classMembers)
				: classVisitor;

		return new AddValidationToConstructorsClassVisitor(classVisitor2, codeFragment, codeFragmentMethods,
				classMembers);
	}

}
