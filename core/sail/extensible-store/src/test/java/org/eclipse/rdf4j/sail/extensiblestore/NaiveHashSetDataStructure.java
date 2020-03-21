/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NaiveHashSetDataStructure implements DataStructureInterface {

	Set<ExtensibleStatement> statements = ConcurrentHashMap.newKeySet();

	public static Consumer<ExtensibleStatement> added = (a) -> {
	};
	public static Consumer<ExtensibleStatement> removed = (a) -> {
	};
	public static volatile boolean halt = false;

	@Override
	synchronized public void addStatement(long transactionId, ExtensibleStatement statement) {
		if (halt) {
			throw new RuntimeException("Halted");
		}
		statements.add(statement);
		added.accept(statement);

	}

	@Override
	synchronized public void removeStatement(long transactionId, ExtensibleStatement statement) {
		if (halt) {
			throw new RuntimeException("Halted");
		}
		statements.remove(statement);
		removed.accept(statement);

	}

	@Override
	synchronized public CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(
			long transactionId,
			Resource subject,
			IRI predicate,
			Value object, boolean inferred, Resource... context) {
		if (halt) {
			throw new RuntimeException("Halted");
		}
		return new FilteringIteration<>(
				new IteratorIteration<>(statements.iterator()), subject, predicate, object, inferred, context);
	}

	@Override
	public void flushForReading(long transactionId) {
		if (halt) {
			throw new RuntimeException("Halted");
		}

	}

	@Override
	public void init() {
		if (halt) {
			throw new RuntimeException("Halted");
		}

	}

	@Override
	public void flushForCommit(long transactionId) {
		if (halt) {
			throw new RuntimeException("Halted");
		}

	}

	@Override
	public long getEstimatedSize() {
		return statements.size();
	}
}
