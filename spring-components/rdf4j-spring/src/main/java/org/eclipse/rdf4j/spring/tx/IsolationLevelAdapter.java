package org.eclipse.rdf4j.spring.tx;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.sail.Sail;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;

public class IsolationLevelAdapter {
	static IsolationLevel adaptToRdfIsolation(Sail sail, int springIsolation) {
		switch (springIsolation) {
		case TransactionDefinition.ISOLATION_DEFAULT:
			return sail.getDefaultIsolationLevel();
		case TransactionDefinition.ISOLATION_READ_COMMITTED:
			return determineIsolationLevel(sail, IsolationLevels.READ_COMMITTED);
		case TransactionDefinition.ISOLATION_READ_UNCOMMITTED:
			return determineIsolationLevel(sail, IsolationLevels.READ_UNCOMMITTED);
		case TransactionDefinition.ISOLATION_REPEATABLE_READ:
			throw new InvalidIsolationLevelException(
					"Unsupported isolation level for sail: " + sail + ": " + springIsolation);
		case TransactionDefinition.ISOLATION_SERIALIZABLE:
			return determineIsolationLevel(sail, IsolationLevels.SERIALIZABLE);
		default:
			throw new InvalidIsolationLevelException(
					"Unsupported isolation level for sail: " + sail + ": " + springIsolation);
		}
	}

	private static IsolationLevel determineIsolationLevel(
			Sail sail, IsolationLevel isolationLevel) {
		if (sail.getSupportedIsolationLevels().contains(isolationLevel)) {
			return isolationLevel;
		} else {
			throw new InvalidIsolationLevelException(
					"Unsupported isolation level for sail: " + sail + ": " + isolationLevel);
		}
	}
}
