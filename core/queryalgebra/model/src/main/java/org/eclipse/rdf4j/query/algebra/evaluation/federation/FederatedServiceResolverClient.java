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

/**
 * Interface used by {@link org.eclipse.rdf4j.sail.config.SailFactory} and
 * {@link org.eclipse.rdf4j.repository.config.RepositoryFactory} that can make external SERVICE calls.
 *
 * @author James Leigh
 */
public interface FederatedServiceResolverClient {

	/**
	 * Sets the {@link FederatedServiceResolver} to use for this client.
	 *
	 * @param resolver The resolver to use.
	 */
	void setFederatedServiceResolver(FederatedServiceResolver resolver);
}
