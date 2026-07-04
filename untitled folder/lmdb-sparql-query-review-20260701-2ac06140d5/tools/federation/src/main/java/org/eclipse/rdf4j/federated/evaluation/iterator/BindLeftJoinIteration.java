/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.List;
import java.util.ListIterator;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * A {@link LookAheadIteration} for processing bind left join results (i.e., result of joining OPTIONAL clauses)
 *
 * Algorithm:
 *
 * <ul>
 * <li>execute left bind join using regular bound join query</li>
 * <li>process result iteration similar to {@link BoundJoinVALUESConversionIteration}</li>
 * <li>remember seen set of bindings (using index) and add original bindings to those, i.e. put to result return all
 * non-seen bindings directly from the input</li>
 *
 *
 * @author Andreas Schwarte
 */
public class BindLeftJoinIteration extends LookAheadIteration<BindingSet> {

	protected final CloseableIteration<BindingSet> iter;
	protected final List<BindingSet> bindings;

	protected IntHashSet seenBindingIndexes = new IntHashSet();
	protected final ListIterator<BindingSet> bindingsIterator;

	public BindLeftJoinIteration(CloseableIteration<BindingSet> iter,
			List<BindingSet> bindings) {
		this.iter = iter;
		this.bindings = bindings;
		this.bindingsIterator = bindings.listIterator();
	}

	@Override
	protected BindingSet getNextElement() {

		if (iter.hasNext()) {
			var bIn = iter.next();
			int bIndex = ((Literal) bIn.getValue(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME)).intValue();
			seenBindingIndexes.add(bIndex);
			return convert(bIn, bIndex);
		}

		while (bindingsIterator.hasNext()) {
			if (seenBindingIndexes.contains(bindingsIterator.nextIndex())) {
				// the binding was already processed as part of the optional
				bindingsIterator.next();
				continue;
			}
			return bindingsIterator.next();
		}

		return null;
	}

	@Override
	protected void handleClose() {
		iter.close();
	}

	protected BindingSet convert(BindingSet bIn, int bIndex) throws QueryEvaluationException {
		QueryBindingSet res = new QueryBindingSet();
		for (Binding b : bIn) {
			if (b.getName().equals(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME)) {
				continue;
			}
			res.addBinding(b);
		}
		for (Binding bs : bindings.get(bIndex)) {
			res.setBinding(bs);
		}
		return res;
	}

}
