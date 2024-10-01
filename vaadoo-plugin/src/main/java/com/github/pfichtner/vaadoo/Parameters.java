package com.github.pfichtner.vaadoo;

import static java.util.Arrays.asList;

import java.util.Iterator;

import net.bytebuddy.jar.asm.Type;

class Parameters implements Iterable<ParameterInfo> {

	final String methodDescriptor;
	final ParameterInfo[] infos;
	final Type[] argumentTypes;

	public Parameters(String methodDescriptor) {
		this.methodDescriptor = methodDescriptor;
		this.argumentTypes = Type.getArgumentTypes(methodDescriptor);
		this.infos = new ParameterInfo[argumentTypes.length];
		int offset = 0;
		for (int i = 0; i < argumentTypes.length; i++) {
			var type = argumentTypes[i];
			infos[i] = new ParameterInfo(i).offset(offset).type(type);
			offset += type.getSize();
		}
	}

	static Parameters fromDescriptor(String methodDescriptor) {
		return new Parameters(methodDescriptor);
	}

	ParameterInfo firstUnamed() {
		for (int i = 0; i < infos.length; i++) {
			var parameterInfo = infos[i];
			if (parameterInfo.name() == null) {
				return parameterInfo;
			}
		}
		throw new IllegalStateException("all elements are named");
	}

	public ParameterInfo byOffset(int offset) {
		for (int i = 0; i < infos.length; i++) {
			var parameterInfo = infos[i];
			if (parameterInfo.offset() >= offset) {
				return parameterInfo;
			}
		}
		throw new IllegalStateException("offset exceeds max");
	}

	public ParameterInfo byIndex(int parameterIdx) {
		return infos[parameterIdx];
	}

	@Override
	public Iterator<ParameterInfo> iterator() {
		return asList(this.infos).iterator();
	}

	public int size() {
		return this.infos.length;
	}

}