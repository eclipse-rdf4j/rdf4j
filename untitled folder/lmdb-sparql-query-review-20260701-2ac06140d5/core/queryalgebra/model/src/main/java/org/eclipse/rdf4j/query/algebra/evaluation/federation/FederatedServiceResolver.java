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

import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * The {@link FederatedServiceResolver} is used to manage a set of {@link FederatedService} instances, which are used to
 * evaluate SERVICE expressions for particular service Urls.
 * <p>
 * Lookup can be done via the serviceUrl using the method {@link #getService(String)}.
 *
 * @author Andreas Schwarte
 * @author James Leigh
 */
public interface FederatedServiceResolver {

	/**
	 * Retrieve the {@link org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService} registered for
	 * serviceUrl. If there is no service registered for serviceUrl, a new
	 * {@link org.eclipse.rdf4j.query.algebra.evaluation.federation.SPARQLFederatedService} is created and registered.
	 *
	 * @param serviceUrl locator for the federation service
	 * @return the {@link org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService}, created fresh if
	 *         necessary
	 * @throws QueryEvaluationException If there was an exception generated while retrieving the service.
	 */
	FederatedService getService(String serviceUrl) throws QueryEvaluationException;

}
