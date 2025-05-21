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
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values;

import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;

public class ScopedQueryValueEvaluationStep implements QueryValueEvaluationStep {

	/**
	 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that are (only)
	 * included because of the depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;
	private final QueryValueEvaluationStep wrapped;

	public ScopedQueryValueEvaluationStep(Set<String> scopeBindingNames, QueryValueEvaluationStep condition) {
		this.scopeBindingNames = scopeBindingNames;
		this.wrapped = condition;
	}

	@Override
	public Value evaluate(BindingSet bindings) {
		BindingSet scopeBindings = createScopeBindings(scopeBindingNames, bindings);

		return wrapped.evaluate(scopeBindings);
	}

	private BindingSet createScopeBindings(Set<String> scopeBindingNames, BindingSet bindings) {
		QueryBindingSet scopeBindings = new QueryBindingSet(scopeBindingNames.size());
		for (String scopeBindingName : scopeBindingNames) {
			Binding binding = bindings.getBinding(scopeBindingName);
			if (binding != null) {
				scopeBindings.addBinding(binding);
			}
		}

		return scopeBindings;
	}
}
