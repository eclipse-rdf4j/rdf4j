/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
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
		// Doubly checked locking is simpler but with Project Loom coming
		// synchronized on this is less interesting.
		private static final VarHandle HANDLE_TO_THE_NOW_LITERAL;
		static {
			try {
				HANDLE_TO_THE_NOW_LITERAL = MethodHandles
						.privateLookupIn(QueryEvaluationContext.Minimal.class, MethodHandles.lookup())
						.findVarHandle(QueryEvaluationContext.Minimal.class, "nowInLiteral", Literal.class);

			} catch (NoSuchFieldException | IllegalAccessException e) {
				throw new UnsupportedOperationException("The nowInLiteral field is missing");
			}
		}
		// The now time as a literal, lazy set if not yet known.
		// No need for volatile as we use the varhandle for this.
		private Literal nowInLiteral;
		private final Dataset dataset;
		private final ValueFactory vf;

		/**
		 * Set the shared now value to an prexisting object
		 * 
		 * @param now     that is shared.
		 * @param dataset that a query should use to the evaluate
		 */
		public Minimal(Literal now, Dataset dataset) {
			super();
			this.nowInLiteral = now;
			this.dataset = dataset;
			this.vf = SimpleValueFactory.getInstance();
		}

		/**
		 * @param dataset that a query should use to the evaluate
		 */
		public Minimal(Dataset dataset) {
			this.dataset = dataset;
			this.vf = SimpleValueFactory.getInstance();
		}

		/**
		 * @param dataset that a query should use to the evaluate
		 */
		public Minimal(Dataset dataset, ValueFactory vf) {
			this.dataset = dataset;
			this.vf = vf;
		}

		@Override
		public Literal getNow() {
			// creating a new date is expensive because it uses the XMLGregorianCalendar implementation which is very
			// complex.
			if (nowInLiteral == null) {
				HANDLE_TO_THE_NOW_LITERAL.compareAndExchange(this, null,
						vf.createLiteral(new Date()));
				nowInLiteral = (Literal) HANDLE_TO_THE_NOW_LITERAL.getVolatile(this);
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
