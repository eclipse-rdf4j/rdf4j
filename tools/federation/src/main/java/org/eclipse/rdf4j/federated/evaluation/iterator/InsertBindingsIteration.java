/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

/**
 * Inserts original bindings into the result.
 *
 * @author Andreas Schwarte
 */
@Deprecated(since = "4.1.0")
public class InsertBindingsIteration extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	protected final BindingSet bindings;

	public InsertBindingsIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter, BindingSet bindings) {
		super(iter);
		this.bindings = bindings;
	}

	@Override
	protected BindingSet convert(BindingSet bIn) throws QueryEvaluationException {
		QueryBindingSet res = new QueryBindingSet(bindings.size() + bIn.size());
		res.addAll(bindings);
		res.addAll(bIn);
		return res;
	}
}
