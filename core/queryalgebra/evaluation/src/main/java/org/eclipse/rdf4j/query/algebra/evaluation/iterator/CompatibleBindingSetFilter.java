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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;

/**
 * @author Arjohn Kampman
 * @deprecated
 */
@Deprecated
public class CompatibleBindingSetFilter extends FilterIteration<BindingSet, QueryEvaluationException> {

	private final BindingSet inputBindings;

	public CompatibleBindingSetFilter(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			BindingSet inputBindings) {
		super(iter);

		assert inputBindings != null;
		this.inputBindings = inputBindings;
	}

	@Override
	protected boolean accept(BindingSet outputBindings) throws QueryEvaluationException {
		return QueryResults.bindingSetsCompatible(inputBindings, outputBindings);
	}
}
