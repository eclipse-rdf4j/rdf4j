/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;

public final class ArrayBindingBasedQueryEvaluationContext implements QueryEvaluationContext {
	private final QueryEvaluationContext context;
	private final String[] allVariables;

	ArrayBindingBasedQueryEvaluationContext(QueryEvaluationContext context, String[] allVariables) {
		this.context = context;
		this.allVariables = allVariables;
	}

	@Override
	public Literal getNow() {
		return context.getNow();
	}

	@Override
	public Dataset getDataset() {
		return context.getDataset();
	}

	@Override
	public ArrayBindingSet createBindingSet() {
		return new ArrayBindingSet(allVariables);
	}

	@Override
	public Function<BindingSet, Boolean> hasBinding(String variableName) {
		Function<ArrayBindingSet, Boolean> directHasVariable = new ArrayBindingSet(allVariables)
				.getDirectHasBinding(variableName);
		return (bs) -> {
			if (bs instanceof ArrayBindingSet) {
				return directHasVariable.apply((ArrayBindingSet) bs);
			} else {
				return bs.hasBinding(variableName);
			}
		};
	}

	@Override
	public Function<BindingSet, Binding> getBinding(String variableName) {
		ArrayBindingSet abs = new ArrayBindingSet(allVariables);
		Function<ArrayBindingSet, Binding> directAccessForVariable = abs
				.getDirectGetBinding(variableName);
		return (bs) -> {
			if (bs instanceof ArrayBindingSet) {
				return directAccessForVariable.apply((ArrayBindingSet) bs);
			} else {
				return bs.getBinding(variableName);
			}
		};
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
		ArrayBindingSet abs = new ArrayBindingSet(allVariables);
		BiConsumer<Value, ArrayBindingSet> directAccessForVariable = abs
				.getDirectSetBinding(variableName);
		return (val, bs) -> {
			if (bs instanceof ArrayBindingSet) {
				directAccessForVariable.accept(val, (ArrayBindingSet) bs);
			} else {
				bs.setBinding(variableName, val);
			}
		};
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> addBinding(String variableName) {
		BiConsumer<Value, ArrayBindingSet> wrapped = new ArrayBindingSet(allVariables)
				.getDirectAddBinding(variableName);
		return (val, bs) -> {
			if (bs instanceof ArrayBindingSet) {
				wrapped.accept(val, (ArrayBindingSet) bs);
			} else {
				bs.addBinding(variableName, val);
			}
		};
	}

	@Override
	public ArrayBindingSet createBindingSet(BindingSet bindings) {
		return new ArrayBindingSet(bindings, allVariables);
	}
}