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
		// The now time in the unix epoch or zero
		private final long nowInMillis;
		// The now time as a literal, lazy set if not yet known.
		private Literal nowInLiteral;

		private final Dataset dataset;

		/**
		 * Set the shared now value to an prexisting object
		 * 
		 * @param now     that is shared.
		 * @param dataset that a query should use to the evaluate
		 */
		public Minimal(Literal now, Dataset dataset) {
			super();
			this.nowInLiteral = now;
			// This is valid because the now field will never be read.
			this.nowInMillis = 0L;
			this.dataset = dataset;
		}

		/**
		 * @param dataset that a query should use to the evaluate
		 */
		public Minimal(Dataset dataset) {
			this.nowInMillis = System.currentTimeMillis();
			this.dataset = dataset;
		}

		@Override
		public Literal getNow() {
			// creating a new date is expensive because it uses the XMLGregorianCalendar implementation which is very
			// complex. So if the nowInLiteral value is null, then we construct a new one based on the long value set
			// nowInMillis.
			// This is thread safe even without a volatile because the literal is instantiated to the same
			// value.
			if (nowInLiteral == null) {
				nowInLiteral = SimpleValueFactory.getInstance().createLiteral(new Date(nowInMillis));
			}

			return nowInLiteral;
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
