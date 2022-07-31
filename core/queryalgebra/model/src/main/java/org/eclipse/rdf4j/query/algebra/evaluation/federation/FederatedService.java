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

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;

/**
 * FederatedService to allow for customized evaluation of SERVICE expression. By default
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.federation.SPARQLFederatedService} is used.
 *
 * @author Andreas Schwarte
 * @author James Leigh
 * @see org.eclipse.rdf4j.query.algebra.evaluation.federation.SPARQLFederatedService
 */
public interface FederatedService {

	/**
	 * <p>
	 * Evaluate the provided SPARQL ASK query at this federated service.
	 * </p>
	 *
	 * <pre>
	 * Expected behavior: evaluate boolean query using the bindings as constraints
	 * </pre>
	 *
	 * @param service  the reference to the service node, contains additional meta information (vars, prefixes)
	 * @param bindings the bindings serving as additional constraints
	 * @param baseUri
	 * @return <code>true</code> if at least one result exists
	 * @throws QueryEvaluationException If there was an exception generated while evaluating the query.
	 */
	boolean ask(Service service, BindingSet bindings, String baseUri) throws QueryEvaluationException;

	/**
	 * <p>
	 * Evaluate the provided SPARQL query at this federated service.
	 * </p>
	 * <p>
	 * <b>Important:</b> The original bindings need to be inserted into the result.
	 * </p>
	 *
	 * <pre>
	 * Expected behavior: evaluate the given SPARQL query using the bindings as constraints
	 * </pre>
	 *
	 * @param service        the reference to the service node, contains additional meta information (vars, prefixes)
	 * @param projectionVars The variables with unknown value that should be projected from this evaluation
	 * @param bindings       the bindings serving as additional constraints
	 * @param baseUri
	 * @return an iteration over the results of the query
	 * @throws QueryEvaluationException If there was an exception generated while evaluating the query.
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> select(Service service, Set<String> projectionVars,
			BindingSet bindings, String baseUri) throws QueryEvaluationException;

	/**
	 * Evaluate the provided SPARQL query at this federated service, possibilities for vectored evaluation.
	 * <p>
	 * <b>Contracts:</b>
	 * <ul>
	 * <li>The original bindings need to be inserted into the result</li>
	 * <li>SILENT service must be dealt with in the method</li>
	 * </ul>
	 * <p>
	 * Compare {@link org.eclipse.rdf4j.query.algebra.evaluation.federation.SPARQLFederatedService} for a reference
	 * implementation
	 * </p>
	 *
	 * @param service  the reference to the service node, contains information to construct the query
	 * @param bindings the bindings serving as additional constraints (for vectored evaluation)
	 * @param baseUri  the baseUri
	 * @return the result of evaluating the query using bindings as constraints, the original bindings need to be
	 *         inserted into the results!
	 * @throws QueryEvaluationException If there was an exception generated while evaluating the query.
	 */
	CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service,
			CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
			throws QueryEvaluationException;

	/**
	 * Method to check if {@link #initialize()} had been called.
	 */
	boolean isInitialized();

	/**
	 * Method to perform any initializations, invoked after construction.
	 *
	 * @throws QueryEvaluationException If there was an exception generated while initializing the service.
	 */
	void initialize() throws QueryEvaluationException;

	/**
	 * Method to perform any shutDown code, invoked at unregistering.
	 *
	 * @throws QueryEvaluationException If there was an exception generated while shutting down the service.
	 */
	void shutdown() throws QueryEvaluationException;
}
