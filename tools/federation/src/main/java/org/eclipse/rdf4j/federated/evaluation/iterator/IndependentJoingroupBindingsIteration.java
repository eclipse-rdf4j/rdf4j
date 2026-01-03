/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Inserts original bindings into the result.
 *
 * @author Andreas Schwarte
 */
@Deprecated(forRemoval = true)
public class IndependentJoingroupBindingsIteration extends LookAheadIteration<BindingSet> {

	protected final BindingSet bindings;
	protected final CloseableIteration<BindingSet> iter;
	protected ArrayList<BindingSet> result = null;
	protected int currentIdx = 0;

	public IndependentJoingroupBindingsIteration(CloseableIteration<BindingSet> iter,
			BindingSet bindings) {
		this.bindings = bindings;
		this.iter = iter;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		if (result == null) {
			result = computeResult();
		}

		if (currentIdx >= result.size()) {
			return null;
		}

		return result.get(currentIdx++);
	}

	protected ArrayList<BindingSet> computeResult() throws QueryEvaluationException {

		List<Binding> a_res = new ArrayList<>();
		List<Binding> b_res = new ArrayList<>();

		// collect results XXX later asynchronously
		// assumes that bindingset of iteration has exactly one binding
		while (iter.hasNext()) {

			BindingSet bIn = iter.next();

			if (bIn.size() != 1) {
				throw new RuntimeException(
						"For this optimization a bindingset needs to have exactly one binding, it has " + bIn.size()
								+ ": " + bIn);
			}

			Binding b = bIn.getBinding(bIn.getBindingNames().iterator().next());
			int bIndex = Integer.parseInt(b.getName().substring(b.getName().lastIndexOf('_') + 1));

			if (bIndex == 0) {
				a_res.add(b);
			} else if (bIndex == 1) {
				b_res.add(b);
			} else {
				throw new RuntimeException("Unexpected binding value.");
			}
		}

		ArrayList<BindingSet> res = new ArrayList<>(a_res.size() * b_res.size());

		for (Binding a : a_res) {
			for (Binding b : b_res) {
				QueryBindingSet newB = new QueryBindingSet(bindings.size() + 2);
				newB.addAll(bindings);
				newB.addBinding(a.getName().substring(0, a.getName().lastIndexOf('_')), a.getValue());
				newB.addBinding(b.getName().substring(0, b.getName().lastIndexOf('_')), b.getValue());
				res.add(newB);
			}
		}

		return res;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		iter.close();
	}
}
