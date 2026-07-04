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
package org.eclipse.rdf4j.http.protocol.transaction.operations;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * An update operation that is part of a transaction.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public interface TransactionOperation {

	/**
	 * Executes this operation on the supplied connection.
	 *
	 * @param con The connection the operation should be performed on.
	 * @throws RepositoryException If such an exception is thrown by the connection while executing the operation.
	 */
	void execute(RepositoryConnection con) throws RepositoryException;
}
