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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Operation to remove statements matching specific pattern of subject, predicate and object.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public class RemoveStatementsOperation extends StatementOperation implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1497684375399016153L;

	/**
	 * Creates a RemoveStatementsOperation.
	 */
	public RemoveStatementsOperation(Resource subj, IRI pred, Value obj, Resource... contexts) {
		super(contexts);

		setSubject(subj);
		setPredicate(pred);
		setObject(obj);
	}

	@Override
	public void execute(RepositoryConnection con) throws RepositoryException {
		con.remove(getSubject(), getPredicate(), getObject(), getContexts());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RemoveStatementsOperation) {
			return super.equals(other);
		}

		return false;
	}
}
