package com.github.pfichtner.vaadoo;

import static com.github.pfichtner.vaadoo.DynamicByteCode.assertException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.assertNoException;
import static com.github.pfichtner.vaadoo.DynamicByteCode.casted;
import static com.github.pfichtner.vaadoo.DynamicByteCode.dynamicClass;
import static com.github.pfichtner.vaadoo.DynamicByteCode.randomConfigWith;
import static com.github.pfichtner.vaadoo.DynamicByteCode.transform;
import static com.github.pfichtner.vaadoo.DynamicByteCode.ConfigEntry.entry;
import static com.github.pfichtner.vaadoo.supplier.CharSequences.Type.BLANKS;
import static com.github.pfichtner.vaadoo.supplier.CharSequences.Type.NON_BLANKS;
import static com.github.pfichtner.vaadoo.supplier.Classes.SubTypes.CHARSEQUENCES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchIllegalStateException;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.github.pfichtner.vaadoo.supplier.CharSequences;
import com.github.pfichtner.vaadoo.supplier.Classes;
import com.github.pfichtner.vaadoo.supplier.Example;

import jakarta.validation.constraints.Email;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.WithNull;

class EmailTest {

	private static final Class<? extends Annotation> ANNO_CLASS = Email.class;

	private static final String VAILD_EMAIL = "pfichtner@users.noreply.github.com";

	@Property
	void oks( //
			@WithNull //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, @ForAll //
			@net.jqwik.web.api.Email String email //
	) throws Exception {
		var config = example == null ? randomConfigWith(entry(CharSequence.class, "param", email).withAnno(ANNO_CLASS))
				: randomConfigWith(entry(example.type(), "param", VAILD_EMAIL).withAnno(ANNO_CLASS));
		var transformed = transform(dynamicClass(config));
		assertNoException(config, transformed);
	}

	@Property
	void nokBecauseNotMatchingCustomRegex( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, @ForAll //
			@net.jqwik.web.api.Email String email //
	) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(
				entry(example.type(), parameterName, VAILD_EMAIL).withAnno(ANNO_CLASS, Map.of("regexp", "")));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, //
				parameterName + " must be a well-formed email address", IllegalArgumentException.class);
	}

	@Property
	void noks( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@ForAll(supplier = CharSequences.class) //
			@CharSequences.Types({ BLANKS, NON_BLANKS }) //
			CharSequence nonValid //
	) throws Exception {
		var parameterName = "param";
		var config = randomConfigWith(
				entry(casted(example.type(), CharSequence.class), parameterName, nonValid).withAnno(ANNO_CLASS));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, //
				parameterName + " must be a well-formed email address", IllegalArgumentException.class);
	}

	@Property
	void customMessage( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(CHARSEQUENCES) //
			Example example, //
			@ForAll String message) throws Exception {
		var config = randomConfigWith(entry(example.type(), "param", "NOT A VALID EMAIL ADDRESS").withAnno(ANNO_CLASS,
				Map.of("message", message)));
		var transformed = transform(dynamicClass(config));
		assertException(config, transformed, message, IllegalArgumentException.class);
	}

	@Property
	void invalidParameterType( //
			@ForAll(supplier = Classes.class) //
			@Classes.Types(not = true, value = CHARSEQUENCES) //
			Example example //
	) throws NoSuchMethodException, ClassNotFoundException {
		var config = randomConfigWith(entry(example.type(), "param", VAILD_EMAIL).withAnno(ANNO_CLASS));
		assertThat(catchIllegalStateException(() -> transform(dynamicClass(config))))
				.hasMessageContainingAll(ANNO_CLASS.getName(), "not allowed")
				.hasMessageContaining(example.type().getName());
	}

}
