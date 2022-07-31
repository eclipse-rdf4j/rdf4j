/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.tx;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.sail.Sail;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;

/**
 * @since 4.0.0
 * @author ameingast@gmail.com
 * @author Florian Kleedorfer
 */
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
