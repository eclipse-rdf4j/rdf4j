package org.eclipse.rdf4j;

public interface TransactionSettingImpl extends TransactionSetting {

	@Override
	default String getName() {
		return getClass().getCanonicalName() + "." + this.toString();
	}

}
