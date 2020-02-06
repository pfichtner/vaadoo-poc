package com.github.pfichtner.bytebuddy.pg._1_manual;

import static com.github.pfichtner.bytebuddy.pg._1_manual.Validator.validate;
import static lombok.AccessLevel.PRIVATE;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.github.pfichtner.bytebuddy.pg._2_own_anno_with_agent.Validate;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
@Validate
public class Person {

	@NotNull
	@Size(min = 2, max = 20)
	private final String name;

	public static Person create(String name) {
		return validate(new Person(name));
	}

}
