/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.join;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;

/**
 * Final adapter that materializes ID-binding records to BindingSets once, at the end of a join chain.
 */
final class LmdbIdFinalBindingSetIteration extends LookAheadIteration<BindingSet> {

	private final RecordIterator input;
	private final IdBindingInfo info;
	private final QueryEvaluationContext context;
	private final BindingSet initial;
	private final ValueStore valueStore;

	LmdbIdFinalBindingSetIteration(RecordIterator input, IdBindingInfo info, QueryEvaluationContext context,
			BindingSet initial, ValueStore valueStore) {
		this.input = input;
		this.info = info;
		this.context = context;
		this.initial = initial;
		this.valueStore = valueStore;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		long[] rec;
		while ((rec = input.next()) != null) {
			MutableBindingSet bs = context.createBindingSet(initial);
			if (info.applyRecord(rec, bs, valueStore)) {
				return bs;
			}
		}
		return null;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		input.close();
	}
}
