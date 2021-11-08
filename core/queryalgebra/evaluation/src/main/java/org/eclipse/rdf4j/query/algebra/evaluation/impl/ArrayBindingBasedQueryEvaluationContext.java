/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.function.BiConsumer;

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
	public java.util.function.Function<BindingSet, Boolean> hasVariableSet(String variableName) {
		java.util.function.Function<ArrayBindingSet, Boolean> directHasVariable = new ArrayBindingSet(allVariables)
				.getDirectHasVariable(variableName);
		return (bs) -> directHasVariable.apply((ArrayBindingSet) bs);
	}

	@Override
	public java.util.function.Function<BindingSet, Binding> getSetVariable(String variableName) {
		ArrayBindingSet abs = new ArrayBindingSet(allVariables);
		java.util.function.Function<ArrayBindingSet, Binding> directAccessForVariable = abs
				.getDirectAccessForVariable(variableName);
		return (bs) -> directAccessForVariable.apply((ArrayBindingSet) bs);
	}

	@Override
	public BiConsumer<Value, MutableBindingSet> addVariable(String variableName) {
		BiConsumer<Value, ArrayBindingSet> wrapped = new ArrayBindingSet(allVariables)
				.getDirectSetterForVariable(variableName);
		return (val, bs) -> wrapped.accept(val, (ArrayBindingSet) bs);
	}

	@Override
	public ArrayBindingSet createBindingSet(BindingSet bindings) {
		return new ArrayBindingSet(bindings, allVariables);
	}
}