package com.github.pfichtner.vaadoo;

import static java.lang.ClassLoader.getSystemClassLoader;
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

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
			var constructor = firstConstructor(transform(sut, SomeRecord.class));
			assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() -> {
				constructor.newInstance(null, sut, null, null, null, null, null, null, null, "", false, false, null,
						null, 0, Long.valueOf(42), Short.valueOf((short) 42));
			}).satisfies(e -> assertThat(e.getCause()).isInstanceOf(NullPointerException.class)
					.hasMessageContaining("someNotEmptyCharSequence must not be null"));
		}
	}

	private String decompile(Path tempDir, Class<?> clazz) throws URISyntaxException, IOException {
		Path sourcePath = Path.of(getClass().getResource("/" + clazz.getName().replace('.', '/') + ".class").toURI());
		Files.copy(sourcePath, tempDir.resolve(clazz.getSimpleName() + ".class"));
		return decompileClass(tempDir, clazz);
//		var transformedClass = transform(sut, SomeClass.class);
//		firstConstructor(transformedClass).newInstance("", "", "", "", false);
	}

	private static Constructor<?> firstConstructor(Class<?> clazz) {
		return Stream.of(clazz.getConstructors()).findFirst().get();
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
