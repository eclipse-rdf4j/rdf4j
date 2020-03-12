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
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;
import org.eclipse.rdf4j.sail.extensiblestore.FilteringIteration;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NaiveHashSetDataStructure implements DataStructureInterface {

	Set<ExtensibleStatement> statements = ConcurrentHashMap.newKeySet();

	@Override
	synchronized public void addStatement(ExtensibleStatement statement) {
		statements.add(statement);

	}

	@Override
	synchronized public void removeStatement(ExtensibleStatement statement) {
		statements.remove(statement);

	}

	@Override
	synchronized public CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(Resource subject,
			IRI predicate,
			Value object, boolean inferred, Resource... context) {
		return new FilteringIteration<>(
				new IteratorIteration<>(statements.iterator()), subject, predicate, object, inferred, context);
	}

	@Override
	public void flushForReading() {

	}

	@Override
	public void init() {

	}

	@Override
	public void flushForCommit() {

	}

	@Override
	public long getEstimatedSize() {
		return statements.size();
	}
}
