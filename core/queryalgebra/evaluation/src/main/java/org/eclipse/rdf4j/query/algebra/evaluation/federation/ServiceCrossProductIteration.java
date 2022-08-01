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
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CrossProductIteration;

/**
 * Iteration which forms the cross product of a list of materialized input bindings with each result obtained from the
 * inner iteration. See {@link SPARQLFederatedService}. Example: <source> inputBindings := {b1, b2, ...} resultIteration
 * := {r1, r2, ...} getNextElement() returns (r1,b1), (r1, b2), ..., (r2, b1), (r2, b2), ... i.e. compute the cross
 * product per result binding </source>
 *
 * @author Andreas Schwarte
 */
@Deprecated
public class ServiceCrossProductIteration extends CrossProductIteration {

	public ServiceCrossProductIteration(CloseableIteration<BindingSet, QueryEvaluationException> resultIteration,
			List<BindingSet> inputBindings) {
		super(resultIteration, inputBindings);
	}

}
