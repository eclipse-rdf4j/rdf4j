/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.model.Statement;

public interface SailConnectionListener {
	/**
	 * Notifies the listener that a statement has been added in a transaction that it has registered itself with.
	 *
	 * @param st       The statement that was added.
	 * @param inferred The flag that indicates whether the statement is inferred or explicit.
	 */
	default void statementAdded(Statement st, boolean inferred) {
		statementAdded(st);
	}

	/**
	 * Notifies the listener that a statement has been removed in a transaction that it has registered itself with.
	 *
	 * @param st       The statement that was removed.
	 * @param inferred The flag that indicates whether the statement was inferred or explicit.
	 */
	default void statementRemoved(Statement st, boolean inferred) {
		statementRemoved(st);
	}

	/**
	 * Notifies the listener that a statement has been added in a transaction that it has registered itself with.
	 *
	 * @param st The statement that was added.
	 */
	@Deprecated(since = "5.2.0", forRemoval = true)
	void statementAdded(Statement st);

	/**
	 * Notifies the listener that a statement has been removed in a transaction that it has registered itself with.
	 *
	 * @param st The statement that was removed.
	 */
	@Deprecated(since = "5.2.0", forRemoval = true)
	void statementRemoved(Statement st);
}
