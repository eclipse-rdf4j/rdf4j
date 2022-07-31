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

/**
 * Inserts original bindings into the result, uses ?__rowIdx to resolve original bindings. See
 * {@link ServiceJoinIterator} and {@link SPARQLFederatedService}.
 *
 * @author Andreas Schwarte
 * @deprecated since 2.3 use {@link org.eclipse.rdf4j.repository.sparql.federation.ServiceJoinConversionIteration}
 */
@Deprecated(since = "2.3", forRemoval = true)
public class ServiceJoinConversionIteration
		extends org.eclipse.rdf4j.repository.sparql.federation.ServiceJoinConversionIteration {

	public ServiceJoinConversionIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			List<BindingSet> bindings) {
		super(iter, bindings);
	}
}
