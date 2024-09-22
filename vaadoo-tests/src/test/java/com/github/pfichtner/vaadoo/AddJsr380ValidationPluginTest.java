package com.github.pfichtner.vaadoo;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Collections.singletonMap;
import static org.approvaltests.Approvals.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssertAlternative;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
	void testGeneratedBytecode(@TempDir Path tempDir) throws Exception {
		verify(decompile(tempDir, SomeClass.class));
	}

	@Test
	void testGeneratedBytecodeOnLombokClass(@TempDir Path tempDir) throws Exception {
		verify(decompile(tempDir, SomeLombokClass.class));
	}

	@Test
	void testGeneratedBytecodeOnRecord() throws Exception {
		try (AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin()) {
			var constructor = firstPublicConstructor(transform(sut, SomeRecord.class));
			assertThatExceptionOfType(InvocationTargetException.class)
					.isThrownBy(() -> constructor.newInstance(null, sut, null, null, null, null, null, null, null, null,
							"1234", "me@example.com", false, false, null, null, 0, Long.valueOf(42),
							Short.valueOf((short) 42)))
					.havingCause().isInstanceOf(NullPointerException.class)
					.withMessage("someNotEmptyCharSequence must not be empty");
		}
	}

	@Test
	void testMandatorShowcase(@TempDir Path tempDir) throws Exception {
		try (AddJsr380ValidationPlugin sut = new AddJsr380ValidationPlugin()) {
			var constructor = firstPublicConstructor(transform(sut, Mandator.class));
			exception(constructor).havingCause().withMessage("args must not be empty");
			exception(constructor, "x").havingCause().withMessage("Mandator muss numerisch sein");
			exception(constructor, "").havingCause().withMessage("id must not be empty");
			exception(constructor, "0").havingCause().withMessage("id must be greater than or equal to 1");
			exception(constructor, "10000").havingCause().withMessage("id must be less than or equal to 9999");
			assertThat(constructor.newInstance(args("9999"))).hasToString("Mandator 9999");

		}
		verify(decompile(tempDir, Mandator.class));
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

	private String decompile(Path tempDir, Class<?> clazz) throws URISyntaxException, IOException {
		Path sourcePath = Path.of(getClass().getResource("/" + clazz.getName().replace('.', '/') + ".class").toURI());
		Files.copy(sourcePath, tempDir.resolve(clazz.getSimpleName() + ".class"));
		return decompileClass(tempDir, clazz);
//		var transformedClass = transform(sut, SomeClass.class);
//		firstConstructor(transformedClass).newInstance("", "", "", "", false);
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

	private String decompileClass(Path destination, Class<?> clazz) throws IOException {
		Map<String, Object> options = Map.of( //
				IFernflowerPreferences.REMOVE_BRIDGE, "true", //
				IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "true" //
		);
		ConsoleDecompiler decompiler = new ConsoleDecompiler(destination.toFile(), options);
		decompiler.addSpace(destination.toFile(), true);
		decompiler.decompileContext();

		Path decompiledJavaFile = destination.resolve(clazz.getSimpleName() + ".java");
		String decompiledSource = Files.readString(decompiledJavaFile);
		Files.delete(decompiledJavaFile);
		return decompiledSource;
	}

}
