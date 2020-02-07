package com.github.pfichtner.bytebuddy.pg._3_no_anno_with_agent;

import static net.bytebuddy.asm.Advice.to;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.declaresField;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import java.lang.instrument.Instrumentation;

import com.github.pfichtner.bytebuddy.pg.agent.Constructor;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

public class Agent {

	public static void premain(String arguments, Instrumentation instrumentation) {
		new AgentBuilder.Default().with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager()) //
				.type(isAnnotated("javax.validation.constraints")
						.or(declaresField(annotatedWith("javax.validation.constraints")) //
								.or(declaresMethod(annotatedWith("javax.validation.constraints"))))) //
				.transform((builder, typeDescription, classLoader, module) -> builder //
						.constructor(any()).intercept(to(Constructor.class)))
				.installOn(instrumentation);
	}

	private static Junction<AnnotationSource> isAnnotated(String packageName) {
		return isAnnotatedWith(target -> target.getPackage().getActualName().equals(packageName));
	}

	private static ElementMatcher<AnnotationSource> annotatedWith(String packageName) {
		return target -> target.getDeclaredAnnotations().stream().anyMatch(a -> isPackage(a, packageName));
	}

	private static boolean isPackage(AnnotationDescription annotationDescription, String packageName) {
		return packageName.equals(annotationDescription.getAnnotationType().getPackage().getActualName());
	}

}
