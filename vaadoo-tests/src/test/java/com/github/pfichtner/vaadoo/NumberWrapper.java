package com.github.pfichtner.vaadoo;

import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class NumberWrapper {

	private final Class<?> type;

	private static class IntWrapper extends NumberWrapper {

		private final int number;

		public IntWrapper(Class<?> type, int number) {
			super(type);
			this.number = number;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		protected long flooredLong() {
			return (long) number;
		}

		@Override
		protected long roundedLong() {
			return (long) number;
		}

		@Override
		public Number add(Number subtrahend) {
			return ((int) number + subtrahend.intValue());
		}

		@Override
		public Number sub(Number subtrahend) {
			return ((int) number - subtrahend.intValue());
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

		public LongWrapper(Class<?> type, long number) {
			super(type);
			this.number = number;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		protected long flooredLong() {
			return (long) number;
		}

		@Override
		protected long roundedLong() {
			return (long) number;
		}

		@Override
		public Number add(Number subtrahend) {
			return ((long) number + subtrahend.longValue());
		}

		@Override
		public Number sub(Number subtrahend) {
			return ((long) number - subtrahend.longValue());
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

		public ShortWrapper(Class<?> type, short number) {
			super(type);
			this.number = number;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		protected long flooredLong() {
			return (long) number;
		}

		@Override
		protected long roundedLong() {
			return (long) number;
		}

		@Override
		public Number add(Number subtrahend) {
			return (short) (number + subtrahend.shortValue());
		}

		@Override
		public Number sub(Number subtrahend) {
			return (short) (number - subtrahend.shortValue());
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

		public ByteWrapper(Class<?> type, byte number) {
			super(type);
			this.number = number;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		protected long flooredLong() {
			return (long) number;
		}

		@Override
		protected long roundedLong() {
			return (long) number;
		}

		@Override
		public Number add(Number subtrahend) {
			return (byte) (number + subtrahend.byteValue());
		}

		@Override
		public Number sub(Number subtrahend) {
			return (byte) (number - subtrahend.byteValue());
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

	private static class BigDecimalWrapper extends NumberWrapper {

		private final BigDecimal number;

		public BigDecimalWrapper(Class<?> type, BigDecimal number) {
			super(type);
			this.number = number;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		protected long flooredLong() {
			return number.setScale(0, FLOOR).longValue();
		}

		@Override
		protected long roundedLong() {
			return number.setScale(0, CEILING).longValue();
		}

		@Override
		public Number add(Number subtrahend) {
			return number.add(new BigDecimal(String.valueOf(subtrahend)));
		}

		@Override
		public Number sub(Number subtrahend) {
			return number.subtract(new BigDecimal(String.valueOf(subtrahend)));
		}

		@Override
		protected boolean isMin() {
			return false;
		}

		@Override
		protected boolean isMax() {
			return false;
		}

	}

	private static class BigIntegerWrapper extends NumberWrapper {

		private final BigInteger number;

		public BigIntegerWrapper(Class<?> type, BigInteger number) {
			super(type);
			this.number = number;
		}

		@Override
		protected Number value() {
			return number;
		}

		@Override
		protected long flooredLong() {
			return number.longValue();
		}

		@Override
		protected long roundedLong() {
			return number.longValue();
		}

		@Override
		public Number add(Number subtrahend) {
			return number.add(new BigInteger(String.valueOf(subtrahend)));
		}

		@Override
		public Number sub(Number subtrahend) {
			return number.subtract(new BigInteger(String.valueOf(subtrahend)));
		}

		@Override
		protected boolean isMin() {
			return false;
		}

		@Override
		protected boolean isMax() {
			return false;
		}

	}

	protected NumberWrapper(Class<?> type) {
		this.type = type;
	}

	public static NumberWrapper numberWrapper(Class<?> clazz, Object value) {
		Number number = (Number) value;
		if (clazz == int.class || clazz == Integer.class) {
			return new IntWrapper(clazz, number.intValue());
		} else if (clazz == long.class || clazz == Long.class) {
			return new LongWrapper(clazz, number.longValue());
		} else if (clazz == short.class || clazz == Short.class) {
			return new ShortWrapper(clazz, number.shortValue());
		} else if (clazz == byte.class || clazz == Byte.class) {
			return new ByteWrapper(clazz, number.byteValue());
		} else if (clazz == BigDecimal.class) {
			return new BigDecimalWrapper(clazz, new BigDecimal(String.valueOf(number)));
		} else if (clazz == BigInteger.class) {
			return new BigIntegerWrapper(clazz, new BigInteger(String.valueOf(number)));
		}
		throw new IllegalStateException("Unsupported type " + clazz + " (" + value + ")");
	}

	@SuppressWarnings("rawtypes")
	protected final Class type() {
		return type;
	}

	protected abstract Number value();

	protected abstract long flooredLong();

	protected abstract long roundedLong();

	protected abstract Number add(Number summand);

	public abstract Number sub(Number subtrahend);

	protected abstract boolean isMin();

	protected abstract boolean isMax();

	@Override
	public String toString() {
		return "NumberWrapper [type=" + type() + ", value=" + value() + "]";
	}

}