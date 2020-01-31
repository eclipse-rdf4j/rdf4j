/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

/**
 * @author Jeen Broekstra
 * @deprecated use {@link SessionManagerDependent} instead.
 */
@Deprecated
public interface SesameClientDependent {

	/**
	 * {@link HttpClientSessionManager} that has been assigned or has been used by this object. The life cycle might not
	 * be or might be tied to this object, depending on whether {@link HttpClientSessionManager} was passed to or
	 * created by this object respectively.
	 * 
	 * @return a {@link HttpClientSessionManager} instance or null
	 */
	HttpClientSessionManager getHttpClientSessionManager();

	/**
	 * @deprecated use {@link #getHttpClientSessionManager()} instead.
	 */
	@Deprecated
	default HttpClientSessionManager getSesameClient() {
		return getHttpClientSessionManager();
	}

	/**
	 * Assign an {@link HttpClientSessionManager} that this object should use. The life cycle of the given
	 * {@link HttpClientSessionManager} is independent of this object. Closing or shutting down this object does not
	 * have any impact on the given client. Callers must ensure that the given client is properly closed elsewhere.
	 * 
	 * @param client
	 */
	void setHttpClientSessionManager(HttpClientSessionManager client);

	/**
	 * @deprecated use {@link #setHttpClientSessionManager()} instead.
	 */
	@Deprecated
	default void setSesameClient(SesameClient client) {
		setHttpClientSessionManager((HttpClientSessionManager) client);
	}
}
