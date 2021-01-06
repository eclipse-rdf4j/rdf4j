/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * A QueryEvaluationContext stores values and methods that are valid throughout the lifetime of a query execution.
 * 
 * A classic case is the case of NOW() evaluation to the same instant for all invocations of that function in one query
 * evaluation.
 * 
 * @author jbollema
 *
 */
public interface QueryEvaluationContext {

	public class Minimal implements QueryEvaluationContext {
		public Minimal(Literal now, Dataset dataset) {
			super();
			this.now = now;
			this.dataset = dataset;
		}

		public Minimal(Dataset dataset) {
			this.dataset = dataset;
			this.now = SimpleValueFactory.getInstance().createLiteral(new Date());
		}

		private final Literal now;
		private final Dataset dataset;

		@Override
		public Literal getNow() {

			return now;
		}

		@Override
		public Dataset getDataset() {
			return dataset;
		}
	}

	/**
	 * @return the shared now;
	 */
	public Literal getNow();

	/**
	 * @return The dataset that this query is operation on.
	 */
	public Dataset getDataset();

	public default BindingSet createBindingSet() {
		return new QueryBindingSet();
	}

	public default Function<BindingSet, Boolean> hasVariableSet(String variableName) {
		return (bs) -> bs.hasBinding(variableName);
	}

	public default Function<BindingSet, Binding> getSetVariable(String variableName) {
		return (bs) -> bs.getBinding(variableName);
	}

	public default BiConsumer<Value, MutableBindingSet> addVariable(String variableName) {
		return (val, bs) -> bs.addBinding(variableName, val);
	}

	public default MutableBindingSet createBindingSet(BindingSet bindings) {
		return new QueryBindingSet(bindings);
	}
}
