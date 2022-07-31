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

import java.io.Serializable;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Operation that clears the whole repository.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public class ClearOperation extends ContextOperation implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -1570893715836564121L;

	public ClearOperation(Resource... contexts) {
		super(contexts);
	}

	@Override
	public void execute(RepositoryConnection con) throws RepositoryException {
		con.clear(getContexts());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ClearOperation) {
			return super.equals(other);
		}

		return false;
	}
}
