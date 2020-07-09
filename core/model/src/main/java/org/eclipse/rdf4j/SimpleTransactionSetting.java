package org.eclipse.rdf4j;

public class SimpleTransactionSetting implements TransactionSetting {

	String name;
	String value;

	public SimpleTransactionSetting(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}
}
