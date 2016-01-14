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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SilentIteration;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

/**
 * Fallback join handler, if the block join can not be performed, e.g. because
 * the BINDINGS clause is not supported by the endpoint. Gets a materialized
 * collection of bindings as input, and has to evaluate the join.
 * 
 * @author Andreas Schwarte
 */
public class ServiceFallbackIteration extends JoinExecutorBase<BindingSet> {

	protected final Service service;

	protected final Set<String> projectionVars;

	protected final FederatedService federatedService;

	protected final Collection<BindingSet> bindings;

	public ServiceFallbackIteration(Service service, Set<String> projectionVars, Collection<BindingSet> bindings,
			FederatedService federatedService)
		throws QueryEvaluationException
	{
		super(null, null, EmptyBindingSet.getInstance());
		this.service = service;
		this.projectionVars = projectionVars;
		this.bindings = bindings;
		this.federatedService = federatedService;
		run();
	}

	@Override
	protected void handleBindings()
		throws Exception
	{

		// NOTE: we do not have to care about SILENT services, as this
		// iteration by itself is wrapped in a silentiteration

		// handle each prepared query individually and add the result to this
		// iteration
		for (BindingSet b : bindings) {
			try {
				CloseableIteration<BindingSet, QueryEvaluationException> result = federatedService.select(
						service, projectionVars, b, service.getBaseURI());
				result = service.isSilent() ? new SilentIteration(result) : result;
				addResult(result);
			} 
			catch (QueryEvaluationException e) {
				// suppress exceptions if silent
				if (service.isSilent()) {
					addResult(new SingletonIteration<BindingSet, QueryEvaluationException>(b));
				} else {
					throw e;
				}
			}			
			catch (RuntimeException e) {
				// suppress special exceptions (e.g. UndeclaredThrowable with wrapped
				// QueryEval) if silent
				if (service.isSilent()) {
					addResult(new SingletonIteration<BindingSet, QueryEvaluationException>(b));
				}
				else {
					throw e;
				}
			}
		}

	}

}
