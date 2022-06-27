package org.eclipse.rdf4j.collection.factory.impl;

import java.util.Objects;

import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.model.Value;

public class DefaultValuePair implements ValuePair {

	private static final long serialVersionUID = 4873622936339338464L;

	private final Value start;
	private final Value end;

	public DefaultValuePair(Value start, Value end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public Value getStartValue() {
		return start;
	}

	@Override
	public Value getEndValue() {
		return end;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + ((end == null) ? 0 : end.hashCode());
		result = 31 * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DefaultValuePair that = (DefaultValuePair) o;

		return Objects.equals(start, that.start) && Objects.equals(end, that.end);
	}
}
