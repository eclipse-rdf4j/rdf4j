/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLQueryBindingSet;

/**
 * Inserts original bindings into the result, uses ?__rowIdx to resolve original bindings. See
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.federation.ServiceJoinIterator} and {@link SPARQLFederatedService}.
 *
 * @author Andreas Schwarte
 */
public class ServiceJoinConversionIteration extends ConvertingIteration<BindingSet, BindingSet> {

	protected final List<BindingSet> bindings;

	private final String rowIdxVar;

	public ServiceJoinConversionIteration(CloseableIteration<BindingSet> iter,
			List<BindingSet> bindings) {
		this(iter, bindings, "__rowIdx");
	}

	/**
	 * @param rowIdxVar the name of the synthetic row-index variable used to correlate remote solutions with the
	 *                  original input bindings. Must not collide with any user-visible variable.
	 */
	public ServiceJoinConversionIteration(CloseableIteration<BindingSet> iter,
			List<BindingSet> bindings, String rowIdxVar) {
		super(iter);
		this.bindings = bindings;
		this.rowIdxVar = rowIdxVar;
	}

	@Override
	protected BindingSet convert(BindingSet bIn) throws QueryEvaluationException {

		// overestimate the capacity
		SPARQLQueryBindingSet res = new SPARQLQueryBindingSet(bIn.size() + bindings.size());

		int bIndex = -1;
		Iterator<Binding> bIter = bIn.iterator();
		while (bIter.hasNext()) {
			Binding b = bIter.next();
			String name = b.getName();
			if (name.equals(rowIdxVar)) {
				bIndex = Integer.parseInt(b.getValue().stringValue());
				continue;
			}
			res.addBinding(b.getName(), b.getValue());
		}

		// should never occur: in such case we would have to create the cross product (which
		// is dealt with in another place)
		if (bIndex == -1) {
			throw new QueryEvaluationException(
					"Invalid join. Probably this is due to non-standard behavior of the SPARQL endpoint. "
							+ "Please report to the developers.");
		}

		// compatible-mapping merge: for a subselect service pattern the remote solution legitimately
		// contains the correlated variables (they are pinned by the injected VALUES clause), so shared
		// names are identical by construction and only absent bindings are added
		for (Binding binding : bindings.get(bIndex)) {
			if (!res.hasBinding(binding.getName())) {
				res.addBinding(binding);
			}
		}
		return res;
	}
}
