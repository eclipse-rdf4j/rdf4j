package org.eclipse.rdf4j.collection.factory.impl;

import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.model.Value;

public class DefaultValuePair implements ValuePair {

	private static final long serialVersionUID = 1L;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValuePair other = (ValuePair) obj;
		if (end == null) {
			if (other.getEndValue() != null)
				return false;
		} else if (!end.equals(other.getEndValue()))
			return false;
		if (start == null) {
			if (other.getStartValue() != null)
				return false;
		} else if (!start.equals(other.getStartValue()))
			return false;
		return true;
	}

}
