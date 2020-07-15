package org.eclipse.rdf4j;

import java.util.Objects;

public class SimpleTransactionSetting implements TransactionSetting {

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimpleTransactionSetting that = (SimpleTransactionSetting) o;
		return Objects.equals(getName(), that.getName()) &&
			Objects.equals(getValue(), that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getValue());
	}
}
