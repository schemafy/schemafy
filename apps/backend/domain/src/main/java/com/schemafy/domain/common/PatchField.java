package com.schemafy.domain.common;

public sealed interface PatchField<T> {

	boolean isPresent();

	T get();

	T orElse(T fallback);

	static <T> PatchField<T> absent() {
		@SuppressWarnings("unchecked")
		Absent<T> instance = (Absent<T>) Absent.INSTANCE;
		return instance;
	}

	static <T> PatchField<T> of(T value) {
		return new Present<>(value);
	}

	record Present<T>(T value) implements PatchField<T> {

		@Override
		public boolean isPresent() {
			return true;
		}

		@Override
		public T get() {
			return value;
		}

		@Override
		public T orElse(T fallback) {
			return value;
		}

	}

	final class Absent<T> implements PatchField<T> {

		@SuppressWarnings("rawtypes")
		private static final Absent INSTANCE = new Absent();

		private Absent() {}

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public T get() {
			throw new IllegalStateException("PatchField is absent");
		}

		@Override
		public T orElse(T fallback) {
			return fallback;
		}

	}

}
