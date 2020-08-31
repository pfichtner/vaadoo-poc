package com.github.pfichtner.bytebuddy.pg._4_no_anno_apt;

import static lombok.AccessLevel.PRIVATE;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class Person {

	@NotNull
	@Size(min = 2, max = 20)
	String name;

}
