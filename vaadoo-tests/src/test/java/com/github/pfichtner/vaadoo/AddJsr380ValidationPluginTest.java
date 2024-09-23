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
			assertThatExceptionOfType(InvocationTargetException.class)
					.isThrownBy(() -> constructor.newInstance(null, sut, null, null, null, null, null, null, null, null,
							null, "1234", "me@example.com", false, false, null, null, 0, Long.valueOf(42),
							Short.valueOf((short) 42)))
					.havingCause().isInstanceOf(NullPointerException.class)
					.withMessage("someNotEmptyCharSequence must not be empty");
		}
	}

	@Test
	void testMandatorShowcase() throws Exception {
		try (AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin()) {
			var constructor = firstPublicConstructor(transform(sut, Mandator.class));
			exception(constructor).havingCause().withMessage("args must not be empty");
			exception(constructor, "x").havingCause().withMessage("Mandator muss numerisch sein");
			exception(constructor, "").havingCause().withMessage("id must not be empty");
			exception(constructor, "0").havingCause().withMessage("id must be greater than or equal to 1");
			exception(constructor, "10000").havingCause().withMessage("id must be less than or equal to 9999");
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
