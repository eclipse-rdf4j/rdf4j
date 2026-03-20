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
package org.eclipse.rdf4j.http.client;

import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;

/**
 * Common interface for objects, such as Repository and RepositoryConnection, that are dependent on
 * {@link RDF4JHttpClient}.
 *
 * @author James Leigh
 */
public interface HttpClientDependent {

	/**
	 * {@link RDF4JHttpClient} that has been assigned or has been used by this object. The life cycle might not be or
	 * might be tied to this object, depending on whether {@link RDF4JHttpClient} was passed to or created by this
	 * object respectively.
	 *
	 * @return an {@link RDF4JHttpClient} instance or null
	 */
	RDF4JHttpClient getHttpClient();

	/**
	 * Assign an {@link RDF4JHttpClient} that this object should use. The life cycle of the given
	 * {@link RDF4JHttpClient} is independent of this object. Closing or shutting down this object does not have any
	 * impact on the given client. Callers must ensure that the given client is properly closed elsewhere.
	 *
	 * @param client
	 */
	void setHttpClient(RDF4JHttpClient client);
}
