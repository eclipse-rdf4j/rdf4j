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

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.federated.evaluation.SparqlFederationEvalStrategy;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Inserts original bindings into the result. This implementation is used for bound joins with VALUES clauses, see
 * {@link SparqlFederationEvalStrategyWithValues}.
 * <p>
 * It is assumed the the query results contain a binding for "?__index" which corresponds to the index in the input
 * mappings. See {@link QueryStringUtil} for details
 * </p>
 *
 * @author Andreas Schwarte
 * @see SparqlFederationEvalStrategy
 * @since 3.0
 */
public class BoundJoinVALUESConversionIteration
		extends ConvertingIteration<BindingSet, BindingSet> {

	/**
	 * The binding name for the index
	 */
	public static final String INDEX_BINDING_NAME = "__index";

	protected final List<BindingSet> bindings;

	public BoundJoinVALUESConversionIteration(CloseableIteration<BindingSet> iter,
			List<BindingSet> bindings) {
		super(iter);
		this.bindings = bindings;
	}

	@Override
	protected BindingSet convert(BindingSet bIn) throws QueryEvaluationException {
		QueryBindingSet res = new QueryBindingSet();
		int bIndex = ((Literal) bIn.getValue(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME)).intValue();
		for (Binding b : bIn) {
			if (b.getName().equals(INDEX_BINDING_NAME)) {
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
