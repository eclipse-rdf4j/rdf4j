/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore.ordered;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;
import org.eclipse.rdf4j.sail.extensiblestore.FilteringIteration;
import org.eclipse.rdf4j.sail.extensiblestore.SortedIteration;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

public class OrderedDataStructure implements DataStructureInterface {

	private static final EmptyIteration<ExtensibleStatement> EMPTY_ITERATION = new EmptyIteration<>();

	Set<ExtensibleStatement> statements = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	synchronized public void addStatement(ExtensibleStatement statement) {
		statements.add(statement);
	}

	@Override
	synchronized public void removeStatement(ExtensibleStatement statement) {
		statements.remove(statement);

	}

	@Override
	synchronized public CloseableIteration<? extends ExtensibleStatement> getStatements(Resource subject,
			IRI predicate,
			Value object, boolean inferred, Resource... context) {
		return new FilteringIteration<>(
				new CloseableIteratorIteration<>(statements.iterator()), subject, predicate, object, inferred, context);
	}

	@Override
	public CloseableIteration<? extends ExtensibleStatement> getStatements(StatementOrder statementOrder,
			Resource subject, IRI predicate, Value object, boolean inferred, Resource... contexts) {
		if (statements.isEmpty()) {
			return EMPTY_ITERATION;
		}
		if (inferred) {
			boolean containsInferred = statements.stream().anyMatch(ExtensibleStatement::isInferred);
			if (!containsInferred)
				return EMPTY_ITERATION;
		}
		return new SortedIteration<>(new FilteringIteration<>(new CloseableIteratorIteration<>(statements.iterator()),
				subject, predicate, object, inferred, contexts), statementOrder);
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

	@Override
	public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, boolean inferred,
			Resource... contexts) {
		return EnumSet.of(StatementOrder.S, StatementOrder.P, StatementOrder.O, StatementOrder.C);
	}

	@Override
	public Comparator<Value> getComparator() {
		return Comparator.comparing(Value::stringValue);
	}
}
