package com.github.pfichtner.vaadoo;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Collections.singletonMap;
import static org.approvaltests.Approvals.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.stream.Stream;

import org.approvaltests.core.Options;
import org.approvaltests.scrubbers.RegExScrubber;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import com.example.Mandator;
import com.example.SomeClass;
import com.example.SomeLombokClass;
import com.example.SomeRecord;
import com.example.custom.ClassWithFizzNumber;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;

class AddJsr380ValidationPluginTest {

	@Test
	void testGeneratedBytecode() throws Exception {
		verify(toJasmin(SomeClass.class), options());
	}

	@Test
	void testGeneratedBytecodeOnLombokClass() throws Exception {
		verify(toJasmin(SomeLombokClass.class), options());
	}

	@Test
	void testGeneratedBytecodeOnRecord() throws Exception {
		try (AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin()) {
			var constructor = firstPublicConstructor(transform(sut, SomeRecord.class));
			Object[] args = defaultArgs(constructor.getParameters());
			args[0] = null;
			assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() -> constructor.newInstance(args))
					.havingCause().isInstanceOf(NullPointerException.class)
					.withMessage("someNotEmptyCharSequence must not be empty");
		}
	}

	@Test
	void testGeneratedBytecodeOnCustomValidatorClass() throws Exception {
		verify(toJasmin(ClassWithFizzNumber.class), options());
	}

	private static Object[] defaultArgs(Parameter[] parameters) {
		Object[] objects = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].getType() == Object.class) {
				objects[i] = new Object();
			} else if (parameters[i].getType() == boolean.class) {
				objects[i] = false;
			} else if (parameters[i].getType() == int.class) {
				objects[i] = 0;
			} else if (parameters[i].getType() == long.class) {
				objects[i] = 0L;
			}
		}
		return objects;
	}

	@Test
	void testMandatorShowcase() throws Exception {
		try (AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin()) {
			var constructor = firstPublicConstructor(transform(sut, Mandator.class));
			exception(constructor).havingCause().withMessage("args must not be empty");
			exception(constructor, "x").havingCause().withMessage("Mandator muss numerisch sein");
			exception(constructor, "").havingCause().withMessage("id must not be empty");
			exception(constructor, "0").havingCause().withMessage("Mandator muss zwischen 1 und 9999 liegen");
			exception(constructor, "10000").havingCause().withMessage("Mandator muss zwischen 1 und 9999 liegen");
			assertThat(constructor.newInstance(args("9999"))).hasToString("Mandator 9999");

		}
		verify(toJasmin(Mandator.class), options());
	}

	private Options options() {
		return new Options().withScrubber(new RegExScrubber("\\A\\.bytecode\\s+.*$\n", "[bytecodeversion]"));
	}

	private ThrowableAssertAlternative<InvocationTargetException> exception(Constructor<?> constructor,
			String... args) {
		return assertThatExceptionOfType(InvocationTargetException.class)
				.isThrownBy(() -> constructor.newInstance(args(args)));
	}

	private static Object[] args(String... args) {
		Object[] result = new Object[1];
		result[0] = args;
		return result;
	}

	private String toJasmin(Class<?> clazz) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try (PrintWriter pw = new PrintWriter(os)) {
			new ClassReader(urlOfClass(clazz).openStream().readAllBytes()).accept(new JasminifierClassAdapter(pw, null),
					SKIP_DEBUG | EXPAND_FRAMES);
		}
		return os.toString();
	}

	private static URL urlOfClass(Class<?> clazz) {
		return clazz.getResource("/" + (clazz.getName().replace('.', File.separatorChar) + ".class"));
	}

	private static Constructor<?> firstPublicConstructor(Class<?> clazz) {
		return Stream.of(clazz.getConstructors()).filter(c -> isPublic(c.getModifiers())).findFirst().get();
	}

	private static Class<?> transform(Plugin plugin, Class<?> clazz) throws ClassNotFoundException {
		var builder = new ByteBuddy().redefine(clazz);
		var transformed = plugin.apply(builder, TypeDescription.ForLoadedType.of(clazz), null).make();
		var classname = clazz.getName();
		var classLoader = new ByteArrayClassLoader(getSystemClassLoader(),
				singletonMap(classname, transformed.getBytes()));
		return classLoader.loadClass(classname);
	}

}
