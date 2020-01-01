/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Collection;

/**
 * An interface to implement a base data structure for the ExtensibleStore. The data structure needs to be thread safe.
 *
 * @author Håvard Mikkelsen Ottestad
 */
@Experimental
public interface DataStructureInterface {

	void addStatement(long transactionId, Statement statement);

	void removeStatement(long transactionId, Statement statement);

	default void addStatement(long transactionId, Collection<Statement> statements) {
		for (Statement statement : statements) {
			addStatement(transactionId, statement);
		}
	}

	default void removeStatement(long transactionId, Collection<Statement> statements) {
		for (Statement statement : statements) {
			removeStatement(transactionId, statement);
		}
	}

	CloseableIteration<? extends Statement, SailException> getStatements(
			long transactionId, Resource subject,
			IRI predicate,
			Value object,
			Resource... context);

	// flush this DataStructure to make added and removed data visible to read operations
	void flushForReading(long transactionId);

	void init();

	default void clear(long transactionId, Resource[] contexts) {
		try (CloseableIteration<? extends Statement, SailException> statements = getStatements(transactionId, null,
				null, null,
				contexts)) {
			while (statements.hasNext()) {
				removeStatement(transactionId, statements.next());
			}
		}
	}

	// flush through to any underlying storage, called by the likes of commit()
	void flushForCommit(long transactionId);

	default boolean removeStatementsByQuery(long transactionId, Resource subj, IRI pred, Value obj,
			Resource[] contexts) {

		boolean deleted = false;
		try (CloseableIteration<? extends Statement, SailException> statements = getStatements(transactionId, subj,
				pred, obj,
				contexts)) {
			while (statements.hasNext()) {
				removeStatement(transactionId, statements.next());
				deleted = true;
			}
		}

		return deleted;

	}
}
