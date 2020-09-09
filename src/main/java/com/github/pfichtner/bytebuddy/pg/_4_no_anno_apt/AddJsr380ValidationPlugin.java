package com.github.pfichtner.bytebuddy.pg._4_no_anno_apt;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.any;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.github.pfichtner.bytebuddy.pg.agent.Constructor;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.SuperMethodCall;

public class AddJsr380ValidationPlugin implements Plugin {

	@Override
	public boolean matches(TypeDescription target) {
		return isPersonInPackage4(target) && //
				(hasJsr380Annotation(target.getDeclaredAnnotations().stream())
						|| hasJsr380Annotation(target.getDeclaredMethods())
						|| hasJsr380Annotation(target.getDeclaredFields()));
	}

	// only transform the com.github.pfichtner.bytebuddy.pg._4_no_anno_apt Person
	private boolean isPersonInPackage4(TypeDescription target) {
		return target.getName().equals(com.github.pfichtner.bytebuddy.pg._4_no_anno_apt.Person.class.getName());
	}

	private boolean hasJsr380Annotation(List<? extends AnnotationSource> annotationSources) {
		return hasJsr380Annotation(
				annotationSources.stream().map(AnnotationSource::getDeclaredAnnotations).flatMap(Collection::stream));
	}

	private boolean hasJsr380Annotation(Stream<AnnotationDescription> annotationDescriptions) {
		return annotationDescriptions.anyMatch(a -> isPackage(a, "javax.validation.constraints"));
	}

	private static boolean isPackage(AnnotationDescription annotationDescription, String packageName) {
		return packageName.equals(annotationDescription.getAnnotationType().getPackage().getActualName());
	}

	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
		return builder.constructor(any()).intercept(SuperMethodCall.INSTANCE.andThen(to(Constructor.class)));
	}

	@Override
	public void close() throws IOException {
		// noop
	}

}