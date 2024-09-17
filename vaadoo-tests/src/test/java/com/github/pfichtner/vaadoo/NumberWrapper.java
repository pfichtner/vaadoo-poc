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

	public static NumberWrapper numberWrapper(Class<?> clazz, Number number) {
		if (clazz == int.class || clazz == Integer.class) {
			return new IntWrapper((Integer) number);
		} else if (clazz == long.class || clazz == Long.class) {
			return new LongWrapper((Long) number);
		}
		throw new IllegalStateException("Unsupported type " + clazz + " (" + number + ")");
	}

	@SuppressWarnings("rawtypes")
	protected abstract Class type();

	protected abstract Number value();

	protected abstract Object add(Number summand);

	public abstract Object sub(Number subtrahend);

	protected abstract boolean isMin();

	protected abstract boolean isMax();

}