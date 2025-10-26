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
package org.eclipse.rdf4j.sail.lmdb;

import java.util.Comparator;
import java.util.Optional;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

/**
 * Extension of {@link QueryEvaluationContext} that exposes LMDB specific resources to evaluation steps.
 */
public class LmdbQueryEvaluationContext extends QueryEvaluationContext.Minimal implements LmdbDatasetContext {

	private final LmdbEvaluationDataset dataset;
	private final ValueStore valueStore;

	public LmdbQueryEvaluationContext(Dataset dataset, ValueFactory valueFactory, Comparator<Value> comparator,
			LmdbEvaluationDataset lmdbDataset, ValueStore valueStore) {
		super(dataset, valueFactory, comparator);
		this.dataset = lmdbDataset;
		this.valueStore = valueStore;
	}

	@Override
	public Optional<LmdbEvaluationDataset> getLmdbDataset() {
		return Optional.ofNullable(dataset);
	}

	@Override
	public Optional<ValueStore> getValueStore() {
		return Optional.ofNullable(valueStore);
	}
}
