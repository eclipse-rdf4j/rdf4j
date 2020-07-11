package org.eclipse.rdf4j;

public interface TransactionSetting {

	default String getName() {
		return getClass().getCanonicalName();
	}

	default String getValue() {
		return toString();
	}
}
