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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
	private List<ConstEntry> constantEntries;

	LmdbIdFinalBindingSetIteration(RecordIterator input, IdBindingInfo info, QueryEvaluationContext context,
			BindingSet initial, ValueStore valueStore, Map<String, Long> constantBindings) {
		this.input = input;
		this.info = info;
		this.context = context;
		this.initial = initial;
		this.valueStore = valueStore;
		this.constantBindings = constantBindings;
		this.constantEntries = Collections.emptyList();
		if (!constantBindings.isEmpty()) {
			List<ConstEntry> entries = new ArrayList<>(constantBindings.size());
			for (Map.Entry<String, Long> e : constantBindings.entrySet()) {
				String name = e.getKey();
				long id = e.getValue();
				Function<BindingSet, Value> getter = context.getValue(name);
				BiConsumer<Value, MutableBindingSet> setter = context.setBinding(name);
				entries.add(new ConstEntry(getter, setter, id));
			}
			this.constantEntries = Collections.unmodifiableList(entries);
		}
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		// No global map; each constant entry resolves lazily once.
		long[] rec;
		while ((rec = input.next()) != null) {
			MutableBindingSet bs = context.createBindingSet(initial);
			if (!constantEntries.isEmpty()) {
				for (ConstEntry ce : constantEntries) {
					// Only set constants not already present
					if (ce.getter.apply(bs) != null) {
						continue;
					}
					if (!ce.valueResolved) {
						try {
							ce.value = LmdbIdJoinSettings.resolveValue(valueStore, ce.id);
						} catch (IOException e) {
							throw new QueryEvaluationException(e);
						}
						ce.valueResolved = true;
					}
					if (ce.value != null) {
						ce.setter.accept(ce.value, bs);
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

	private static final class ConstEntry {
		final Function<BindingSet, Value> getter;
		final BiConsumer<Value, MutableBindingSet> setter;
		final long id;
		boolean valueResolved;
		Value value;

		ConstEntry(Function<BindingSet, Value> getter,
				BiConsumer<Value, MutableBindingSet> setter,
				long id) {
			this.getter = getter;
			this.setter = setter;
			this.id = id;
		}
	}
}
