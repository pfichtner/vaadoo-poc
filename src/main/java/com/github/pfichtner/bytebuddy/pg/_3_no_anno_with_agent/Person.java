package com.github.pfichtner.bytebuddy.pg._3_no_anno_with_agent;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Value;

@Value
public class Person {

	@NotNull
	@Size(min = 2, max = 20)
	private final String name;

}
