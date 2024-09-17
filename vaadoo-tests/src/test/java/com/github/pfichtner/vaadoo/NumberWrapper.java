package com.github.pfichtner.vaadoo;

public abstract class NumberWrapper {

	private static class IntWrapper extends NumberWrapper {

		private final int number;

		public IntWrapper(int number) {
			this.number = number;
		}

		@Override
		protected Class<?> type() {
			return int.class;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		public Object add(Number subtrahend) {
			return number + subtrahend.intValue();
		}

		@Override
		public Object sub(Number subtrahend) {
			return number - subtrahend.intValue();
		}

		@Override
		protected boolean isMax() {
			return number == Integer.MAX_VALUE;
		}

		@Override
		protected boolean isMin() {
			return number == Integer.MIN_VALUE;
		}

	}

	private static class LongWrapper extends NumberWrapper {

		private final long number;

		public LongWrapper(long number) {
			this.number = number;
		}

		@Override
		protected Class<?> type() {
			return long.class;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		public Object add(Number subtrahend) {
			return number + subtrahend.longValue();
		}

		@Override
		public Object sub(Number subtrahend) {
			return number - subtrahend.longValue();
		}

		@Override
		protected boolean isMin() {
			return number == Long.MIN_VALUE;
		}

		@Override
		protected boolean isMax() {
			return number == Long.MAX_VALUE;
		}

	}

	private static class ShortWrapper extends NumberWrapper {

		private final short number;

		public ShortWrapper(short number) {
			this.number = number;
		}

		@Override
		protected Class<?> type() {
			return short.class;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		public Object add(Number subtrahend) {
			return (short) number + subtrahend.shortValue();
		}

		@Override
		public Object sub(Number subtrahend) {
			return (short) number - subtrahend.shortValue();
		}

		@Override
		protected boolean isMin() {
			return number == Short.MIN_VALUE;
		}

		@Override
		protected boolean isMax() {
			return number == Short.MAX_VALUE;
		}

	}

	private static class ByteWrapper extends NumberWrapper {

		private final byte number;

		public ByteWrapper(byte number) {
			this.number = number;
		}

		@Override
		protected Class<?> type() {
			return byte.class;
		}

		@Override
		protected Number value() {
			return (byte) number;
		}

		@Override
		public Object add(Number subtrahend) {
			return (byte) number + subtrahend.shortValue();
		}

		@Override
		public Object sub(Number subtrahend) {
			return (byte) number - subtrahend.shortValue();
		}

		@Override
		protected boolean isMin() {
			return number == Byte.MIN_VALUE;
		}

		@Override
		protected boolean isMax() {
			return number == Byte.MAX_VALUE;
		}

	}

	public static NumberWrapper numberWrapper(Class<?> clazz, Object value) {
		Number number = (Number) value;
		if (clazz == int.class || clazz == Integer.class) {
			return new IntWrapper(number.intValue());
		} else if (clazz == long.class || clazz == Long.class) {
			return new LongWrapper(number.longValue());
		} else if (clazz == short.class || clazz == Short.class) {
			return new ShortWrapper(number.shortValue());
		} else if (clazz == byte.class || clazz == Byte.class) {
			return new ByteWrapper(number.byteValue());
		}
		throw new IllegalStateException("Unsupported type " + clazz + " (" + value + ")");
	}

	@SuppressWarnings("rawtypes")
	protected abstract Class type();

	protected abstract Number value();

	protected abstract Object add(Number summand);

	public abstract Object sub(Number subtrahend);

	protected abstract boolean isMin();

	protected abstract boolean isMax();

	@Override
	public String toString() {
		return "NumberWrapper [type=" + type() + ", value=" + value() + "]";
	}

}