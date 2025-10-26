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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

class LmdbDelegatingQueryEvaluationContext implements QueryEvaluationContext, LmdbDatasetContext {

	private final QueryEvaluationContext delegate;
	private final LmdbEvaluationDataset dataset;
	private final ValueStore valueStore;

	LmdbDelegatingQueryEvaluationContext(QueryEvaluationContext delegate, LmdbEvaluationDataset dataset,
			ValueStore valueStore) {
		this.delegate = delegate;
		this.dataset = dataset;
		this.valueStore = valueStore;
	}

	@Override
	public Comparator<Value> getComparator() {
		return delegate.getComparator();
	}

	@Override
	public org.eclipse.rdf4j.model.Literal getNow() {
		return delegate.getNow();
	}

	@Override
	public Dataset getDataset() {
		return delegate.getDataset();
	}

	@Override
	public MutableBindingSet createBindingSet() {
		return delegate.createBindingSet();
	}

	@Override
	public Predicate<BindingSet> hasBinding(String variableName) {
		return delegate.hasBinding(variableName);
	}

	@Override
	public Function<BindingSet, Binding> getBinding(String variableName) {
		return delegate.getBinding(variableName);
	}

	@Override
	public Function<BindingSet, Value> getValue(String variableName) {
		return delegate.getValue(variableName);
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
		return delegate.setBinding(variableName);
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> addBinding(String variableName) {
		return delegate.addBinding(variableName);
	}

	@Override
	public MutableBindingSet createBindingSet(BindingSet bindings) {
		return delegate.createBindingSet(bindings);
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
