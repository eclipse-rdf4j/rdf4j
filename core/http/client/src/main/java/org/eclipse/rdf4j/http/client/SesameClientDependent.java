/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

/**
 * Common interface for objects, such as Repository and RepositoryConnection,
 * that are dependent on {@link SesameClient}.
 * 
 * @author James Leigh
 */
public interface SesameClientDependent {

	/**
	 * {@link SesameClient} that has been assigned or has been used by this
	 * object. The life cycle might not be or might be tied to this object,
	 * depending on whether {@link SesameClient} was passed to or created by this
	 * object respectively.
	 * 
	 * @return a {@link SesameClient} instance or null
	 */
	SesameClient getSesameClient();

	/**
	 * Assign an {@link SesameClient} that this object should use. The life cycle
	 * of the given {@link SesameClient} is independent of this object. Closing
	 * or shutting down this object does not have any impact on the given client.
	 * Callers must ensure that the given client is properly closed elsewhere.
	 * 
	 * @param client
	 */
	void setSesameClient(SesameClient client);
}
