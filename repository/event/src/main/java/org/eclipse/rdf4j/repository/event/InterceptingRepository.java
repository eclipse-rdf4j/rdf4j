/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.event;

import org.eclipse.rdf4j.repository.Repository;

/**
 * @author Herko ter Horst
 */
public interface InterceptingRepository extends Repository {

	/**
	 * Registers a <tt>RepositoryInterceptor</tt> that will receive notifications of operations that are
	 * performed on this repository.
	 */
	public void addRepositoryInterceptor(RepositoryInterceptor interceptor);

	/**
	 * Removes a registered <tt>RepositoryInterceptor</tt> from this repository.
	 */
	public void removeRepositoryInterceptor(RepositoryInterceptor interceptor);

	/**
	 * Registers a <tt>RepositoryConnectionInterceptor</tt> that will receive notifications of operations that
	 * are performed on this connection.
	 */
	public void addRepositoryConnectionInterceptor(RepositoryConnectionInterceptor interceptor);

	/**
	 * Removes a registered <tt>RepositoryConnectionInterceptor</tt> from this connection.
	 */
	public void removeRepositoryConnectionInterceptor(RepositoryConnectionInterceptor interceptor);
}
