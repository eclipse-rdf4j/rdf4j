/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.util.Collection;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;

/**
 * Fallback join handler, if the block join can not be performed, e.g. because the BINDINGS clause is not
 * supported by the endpoint. Gets a materialized collection of bindings as input, and has to evaluate the
 * join.
 * 
 * @author Andreas Schwarte
 * @deprecated since 2.3 use {@link org.eclipse.rdf4j.repository.sparql.federation.ServiceFallbackIteration}
 */
@Deprecated
public class ServiceFallbackIteration
		extends org.eclipse.rdf4j.repository.sparql.federation.ServiceFallbackIteration
{

	public ServiceFallbackIteration(Service service, Set<String> projectionVars,
			Collection<BindingSet> bindings, FederatedService federatedService)
		throws QueryEvaluationException
	{
		super(service, projectionVars, bindings, federatedService);
	}
}
