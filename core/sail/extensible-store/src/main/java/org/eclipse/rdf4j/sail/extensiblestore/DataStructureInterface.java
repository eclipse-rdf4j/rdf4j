/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import java.util.Collection;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

/**
 * An interface to implement a base data structure for the ExtensibleStore. The data structure needs to be thread safe.
 *
 * @author Håvard Mikkelsen Ottestad
 */
@Experimental
public interface DataStructureInterface {

	void addStatement(ExtensibleStatement statement);

	void removeStatement(ExtensibleStatement statement);

	default void addStatement(Collection<ExtensibleStatement> statements) {
		for (ExtensibleStatement statement : statements) {
			addStatement(statement);
		}
	}

	default void removeStatement(Collection<ExtensibleStatement> statements) {
		for (ExtensibleStatement statement : statements) {
			removeStatement(statement);
		}
	}

	CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(
			Resource subject,
			IRI predicate,
			Value object,
			boolean inferred,
			Resource... context);

	// flush this DataStructure to make added and removed data visible to read operations
	void flushForReading();

	void init();

	default void clear(boolean inferred, Resource[] contexts) {
		try (CloseableIteration<? extends ExtensibleStatement, SailException> statements = getStatements(null, null,
				null,
				inferred, contexts)) {
			while (statements.hasNext()) {
				removeStatement(statements.next());
			}
		}
	}

	// flush through to any underlying storage, called by the likes of commit()
	void flushForCommit();

	default boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource[] contexts) {

		boolean deleted = false;
		try (CloseableIteration<? extends ExtensibleStatement, SailException> statements = getStatements(subj, pred,
				obj,
				inferred, contexts)) {
			while (statements.hasNext()) {
				removeStatement(statements.next());
				deleted = true;
			}
		}

		return deleted;

	}

	default long getEstimatedSize() {
		long inferred = getStatements(null, null, null, true).stream().count();
		long explicit = getStatements(null, null, null, false).stream().count();
		return inferred + explicit;
	}
}
