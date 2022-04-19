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
import java.util.function.Predicate;

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
 */
public interface QueryEvaluationContext {

	public class Minimal implements QueryEvaluationContext {
		public Minimal(Literal now, Dataset dataset) {
			super();
			this.nowL = now;
			// This is valid because the now field will never be read.
			this.now = 0L;
			this.dataset = dataset;
		}

		public Minimal(Dataset dataset) {
			this.now = System.currentTimeMillis();
			this.dataset = dataset;
		}

		private final long now;
		private Literal nowL;
		private final Dataset dataset;

		@Override
		public Literal getNow() {
			// creating a new date is expensive because it uses the XMLGregorianCalendar implementation which is very
			// complex. This is thread safe even without a volatile because the literal is instantiated to the same
			// value.
			if (nowL == null) {
				nowL = SimpleValueFactory.getInstance().createLiteral(new Date(now));
			}

			return nowL;
		}

		@Override
		public Dataset getDataset() {
			return dataset;
		}
	}

	/**
	 * @return the shared now;
	 */
	Literal getNow();

	/**
	 * @return The dataset that this query is operation on.
	 */
	Dataset getDataset();

	default MutableBindingSet createBindingSet() {
		return new QueryBindingSet();
	}

	default Predicate<BindingSet> hasBinding(String variableName) {
		return (bs) -> bs.hasBinding(variableName);
	}

	default Function<BindingSet, Binding> getBinding(String variableName) {
		return (bs) -> bs.getBinding(variableName);
	}

	default Function<BindingSet, Value> getValue(String variableName) {
		Function<BindingSet, Binding> getBinding = getBinding(variableName);
		return (bs) -> {
			Binding binding = getBinding.apply(bs);
			if (binding == null) {
				return null;
			} else {
				return binding.getValue();
			}
		};
	}

	default BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
		return (val, bs) -> bs.setBinding(variableName, val);
	}

	default BiConsumer<Value, MutableBindingSet> addBinding(String variableName) {
		return (val, bs) -> bs.addBinding(variableName, val);
	}

	default MutableBindingSet createBindingSet(BindingSet bindings) {
		return new QueryBindingSet(bindings);
	}
}
