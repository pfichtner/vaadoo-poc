package com.github.pfichtner.vaadoo;

import net.bytebuddy.jar.asm.MethodVisitor;

public interface CodeEmitter {

	void addNullCheck(MethodVisitor mv, ParameterInfo parameter);
	
	void addNotNullCheck(MethodVisitor mv, ParameterInfo parameterInfo);

	void addNotEmptyCheck(MethodVisitor mv, ParameterInfo parameterInfo);

	void addNotBlankCheck(MethodVisitor mv, ParameterInfo parameterInfo);

	void addIsTrueCheck(MethodVisitor mv, ParameterInfo parameterInfo);

	void addIsFalseCheck(MethodVisitor mv, ParameterInfo parameterInfo);

	void addMinCheck(MethodVisitor mv, ParameterInfo parameter);

}