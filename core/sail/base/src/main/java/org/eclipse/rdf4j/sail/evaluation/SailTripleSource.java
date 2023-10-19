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
package org.eclipse.rdf4j.sail.evaluation;

import java.util.Comparator;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.TripleSourceIterationWrapper;

public class SailTripleSource implements TripleSource {

	private final SailConnection conn;

	private final boolean includeInferred;

	private final ValueFactory vf;

	public SailTripleSource(SailConnection conn, boolean includeInferred, ValueFactory valueFactory) {
		this.conn = conn;
		this.includeInferred = includeInferred;
		this.vf = valueFactory;
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException {
		CloseableIteration<? extends Statement> iter = null;
		try {
			iter = conn.getStatements(subj, pred, obj, includeInferred, contexts);
			if (iter instanceof EmptyIteration) {
				return iter;
			}
			return new TripleSourceIterationWrapper<>(iter);
		} catch (Throwable t) {
			if (iter != null) {
				iter.close();
			}
			if (t instanceof SailException) {
				throw new QueryEvaluationException(t);
			}
			throw t;
		}
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(StatementOrder order, Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException {
		CloseableIteration<? extends Statement> iter = null;
		try {
			iter = conn.getStatements(order, subj, pred, obj, includeInferred, contexts);
			if (iter instanceof EmptyIteration) {
				return iter;
			}
			return new TripleSourceIterationWrapper<>(iter);
		} catch (Throwable t) {
			if (iter != null) {
				iter.close();
			}
			if (t instanceof SailException) {
				throw new QueryEvaluationException(t);
			}
			throw t;
		}
	}

	@Override
	public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return conn.getSupportedOrders(subj, pred, obj, contexts);
	}

	@Override
	public ValueFactory getValueFactory() {
		return vf;
	}

	@Override
	public Comparator<Value> getComparator() {
		return conn.getComparator();
	}
}
