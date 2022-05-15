/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Inserts original bindings into the result.
 *
 * @author Andreas Schwarte
 */
public class IndependentJoingroupBindingsIteration3 extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	// a pattern matcher for the binding resolver, pattern: myVar_%outerID%#bindingId, e.g. name_0#0
	protected static final Pattern pattern = Pattern.compile("(.*)_(.*)_(.*)");

	protected final List<BindingSet> bindings;
	protected final CloseableIteration<BindingSet, QueryEvaluationException> iter;
	protected ArrayList<BindingSet> result = null;
	protected int currentIdx = 0;

	public IndependentJoingroupBindingsIteration3(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			List<BindingSet> bindings) {
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

		// underlying arraylist serves as map, index corresponds to bindings index (i.e. at most bindings.size() - 1)
		// a_res[0] = { v_0#0-1; v_0#0-2; ... }
		// a_res[1] = { v_0#1-1; v_0#1-2; ... }
		ArrayList<LinkedList<BindingInfo>> a_res = new ArrayList<>(bindings.size());
		ArrayList<LinkedList<BindingInfo>> b_res = new ArrayList<>(bindings.size());

		// we assume that each binding returns at least one result for each statement
		// => create lists in advance to avoid checking later on
		for (int i = 0; i < bindings.size(); i++) {
			a_res.add(new LinkedList<>());
			b_res.add(new LinkedList<>());
		}

		// assumes that bindingset of iteration has exactly one binding
		while (iter.hasNext()) {

			BindingSet bIn = iter.next();

			if (bIn.size() != 1) {
				throw new RuntimeException(
						"For this optimization a bindingset needs to have exactly one binding, it has " + bIn.size()
								+ ": " + bIn);
			}

			Binding b = bIn.getBinding(bIn.getBindingNames().iterator().next());

			// name is something like myVar_%outerID%_bindingId, e.g. name_0_0
			Matcher m = pattern.matcher(b.getName());
			if (!m.find()) {
				throw new QueryEvaluationException("Unexpected pattern for binding name: " + b.getName());
			}

			BindingInfo bInfo = new BindingInfo(m.group(1), Integer.parseInt(m.group(3)), b.getValue());
			int bIndex = Integer.parseInt(m.group(2));

//			int tmp = b.getName().indexOf("_");
//			String pattern = b.getName().substring(tmp+1);
//			String split[] = pattern.split("_");
//
//			int bIndex = Integer.parseInt(split[0]);
//			int bindingsIdx = Integer.parseInt(split[1]);
//			BindingInfo bInfo = new BindingInfo(b.getName().substring(0, tmp), bindingsIdx, b.getValue());

			// add a new binding info to the correct result list
			if (bIndex == 0) {
				a_res.get(bInfo.bindingsIdx).add(bInfo);
			} else if (bIndex == 1) {
				b_res.get(bInfo.bindingsIdx).add(bInfo);
			} else {
				throw new RuntimeException("Unexpected binding value.");
			}
		}

		// TODO think about a better upper bound or use linked list
		ArrayList<BindingSet> res = new ArrayList<>(2 * bindings.size());

		for (int a_idx = 0; a_idx < a_res.size(); a_idx++) {
			LinkedList<BindingInfo> a_list = a_res.get(a_idx);
			for (BindingInfo a : a_list) {
				for (BindingInfo b : b_res.get(a_idx)) {
					QueryBindingSet newB = new QueryBindingSet(bindings.size() + 2);
					newB.addBinding(a.name, a.value);
					newB.addBinding(b.name, b.value);
					newB.addAll(bindings.get(a.bindingsIdx));
					res.add(newB);
				}
			}
		}

		return res;
	}

	protected class BindingInfo {
		public final String name;
		public final int bindingsIdx;
		public final Value value;

		public BindingInfo(String name, int bindingsIdx, Value value) {
			super();
			this.name = name;
			this.bindingsIdx = bindingsIdx;
			this.value = value;
		}

		@Override
		public String toString() {
			return name + ":" + value.stringValue();
		}
	}

}
