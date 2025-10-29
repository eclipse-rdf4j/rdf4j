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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
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
	private final Map<String, Long> constantBindings;
	private Map<String, Value> constantValues;

	LmdbIdFinalBindingSetIteration(RecordIterator input, IdBindingInfo info, QueryEvaluationContext context,
			BindingSet initial, ValueStore valueStore, Map<String, Long> constantBindings) {
		this.input = input;
		this.info = info;
		this.context = context;
		this.initial = initial;
		this.valueStore = valueStore;
		this.constantBindings = constantBindings;
		this.constantValues = null; // lazily initialized
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		// Lazily resolve constant IDs to Values once to avoid repeated lookups
		if (constantValues == null && !constantBindings.isEmpty()) {
			Map<String, Value> resolved = new HashMap<>();
			for (Map.Entry<String, Long> entry : constantBindings.entrySet()) {
				try {
					Value v = valueStore.getLazyValue(entry.getValue());
					if (v != null) {
						resolved.put(entry.getKey(), v);
					}
				} catch (IOException e) {
					throw new QueryEvaluationException(e);
				}
			}
			constantValues = Collections.unmodifiableMap(resolved);
		}
		long[] rec;
		while ((rec = input.next()) != null) {
			MutableBindingSet bs = context.createBindingSet(initial);
			if (!constantBindings.isEmpty()) {
				for (Map.Entry<String, Long> entry : constantBindings.entrySet()) {
					String name = entry.getKey();
					if (bs.hasBinding(name)) {
						continue;
					}
					Value constantValue = constantValues.get(name);
					if (constantValue != null) {
						bs.setBinding(name, constantValue);
					}
				}
			}
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
