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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;

/**
 * Iterator for efficient SERVICE evaluation (vectored). SERVICE is the right handside argument of this join.
 *
 * @author Andreas Schwarte
 */
public class ServiceJoinIterator extends JoinExecutorBase<BindingSet> {

	protected Service service;

	protected EvaluationStrategy strategy;

	/**
	 * Construct a service join iteration to use vectored evaluation. The constructor automatically starts evaluation.
	 *
	 * @param leftIter
	 * @param service
	 * @param bindings
	 * @param strategy
	 * @throws QueryEvaluationException
	 */
	public ServiceJoinIterator(CloseableIteration<BindingSet, QueryEvaluationException> leftIter, Service service,
			BindingSet bindings, EvaluationStrategy strategy) throws QueryEvaluationException {
		super(leftIter, service, bindings);
		this.service = service;
		this.strategy = strategy;
		run();
	}

	@Override
	protected void handleBindings() throws Exception {
		Var serviceRef = service.getServiceRef();

		String serviceUri;
		if (serviceRef.hasValue()) {
			serviceUri = serviceRef.getValue().stringValue();
		} else {
			// case 2: the service ref is not defined beforehand
			// => use a fallback to the naive evaluation.
			// exceptions occurring here must NOT be silenced!
			while (!isClosed() && leftIter.hasNext()) {
				addResult(strategy.evaluate(service, leftIter.next()));
			}
			return;
		}

		// use vectored evaluation
		FederatedService fs = strategy.getService(serviceUri);
		addResult(fs.evaluate(service, leftIter, service.getBaseURI()));
	}
}
