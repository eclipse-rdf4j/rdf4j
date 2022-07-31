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
 * Operation to add a statement.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public class AddStatementOperation extends StatementOperation implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7055177153036638975L;

	/**
	 * Create an AddStatementOperation.
	 */
	public AddStatementOperation(Resource subj, IRI pred, Value obj, Resource... contexts) {
		super(contexts);

		assert subj != null : "subj must not be null";
		assert pred != null : "pred must not be null";
		assert obj != null : "obj must not be null";

		setSubject(subj);
		setPredicate(pred);
		setObject(obj);
	}

	@Override
	public void execute(RepositoryConnection con) throws RepositoryException {
		con.add(getSubject(), getPredicate(), getObject(), getContexts());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AddStatementOperation) {
			return super.equals(other);
		}

		return false;
	}
}
