/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

class ConnectionHelper {

	static CloseableIteration<Statement, SailException> getCloseableIteration(
			RepositoryResult<Statement> repositoryResults) {

		return new CloseableIteration<>() {

			@Override
			public boolean hasNext() throws SailException {
				return repositoryResults.hasNext();
			}

			@Override
			public Statement next() throws SailException {
				return repositoryResults.next();
			}

			@Override
			public void remove() throws SailException {
				repositoryResults.remove();
			}

			@Override
			public void close() throws SailException {
				repositoryResults.close();
			}
		};

	}

	static void transferStatements(SailConnection from, TransferStatement transfer) {

		try (CloseableIteration<? extends Statement, SailException> statements = from
				.getStatements(null, null, null, false)) {

			while (statements.hasNext()) {
				Statement next = statements.next();

				transfer.transfer(next.getSubject(), next.getPredicate(),
						next.getObject(), next.getContext());

			}
		}

	}

	static boolean isEmpty(SailConnection connection) {
		return !connection.hasStatement(null, null, null, false);
	}

}

interface TransferStatement {
	void transfer(Resource subject, IRI predicate, Value object, Resource context);
}
