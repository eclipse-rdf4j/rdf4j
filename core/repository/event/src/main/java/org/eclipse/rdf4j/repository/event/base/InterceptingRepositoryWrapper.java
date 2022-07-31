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
package org.eclipse.rdf4j.repository.event.base;

import java.io.File;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.event.InterceptingRepository;
import org.eclipse.rdf4j.repository.event.InterceptingRepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionInterceptor;
import org.eclipse.rdf4j.repository.event.RepositoryInterceptor;

/**
 * Wrapper that notifies interceptors of events on Repositories before they happen. Any interceptor can block the
 * operation by returning true from the relevant notification method. To do so will also cause the notification process
 * to stop, i.e. no other interceptors will be notified. The order in which interceptors are notified is unspecified.
 *
 * @author Herko ter Horst
 * @see InterceptingRepositoryConnectionWrapper
 */
public class InterceptingRepositoryWrapper extends RepositoryWrapper implements InterceptingRepository {

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean activated;

	private final Set<RepositoryInterceptor> interceptors = new CopyOnWriteArraySet<>();

	private final Set<RepositoryConnectionInterceptor> conInterceptors = new CopyOnWriteArraySet<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public InterceptingRepositoryWrapper() {
		super();
	}

	public InterceptingRepositoryWrapper(Repository delegate) {
		super(delegate);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Registers a <var>RepositoryInterceptor</var> that will receive notifications of operations that are performed on
	 * this repository.
	 */
	@Override
	public void addRepositoryInterceptor(RepositoryInterceptor interceptor) {
		interceptors.add(interceptor);
		activated = true;
	}

	/**
	 * Removes a registered <var>RepositoryInterceptor</var> from this repository.
	 */
	@Override
	public void removeRepositoryInterceptor(RepositoryInterceptor interceptor) {
		interceptors.remove(interceptor);
		activated = !interceptors.isEmpty();
	}

	/**
	 * Registers a <var>RepositoryConnectionInterceptor</var> that will receive notifications of operations that are
	 * performed on any connections that are created by this repository.
	 */
	@Override
	public void addRepositoryConnectionInterceptor(RepositoryConnectionInterceptor interceptor) {
		conInterceptors.add(interceptor);
	}

	/**
	 * Removes a registered <var>RepositoryConnectionInterceptor</var> from this repository.
	 */
	@Override
	public void removeRepositoryConnectionInterceptor(RepositoryConnectionInterceptor interceptor) {
		conInterceptors.remove(interceptor);
	}

	@Override
	public InterceptingRepositoryConnection getConnection() throws RepositoryException {
		RepositoryConnection conn = getDelegate().getConnection();
		if (activated) {
			boolean denied = false;

			for (RepositoryInterceptor interceptor : interceptors) {
				denied = interceptor.getConnection(getDelegate(), conn);
				if (denied) {
					break;
				}
			}
			if (denied) {
				conn = null;
			}
		}
		if (conn == null) {
			return null;
		}

		InterceptingRepositoryConnection iconn = new InterceptingRepositoryConnectionWrapper(this, conn);
		for (RepositoryConnectionInterceptor conInterceptor : conInterceptors) {
			iconn.addRepositoryConnectionInterceptor(conInterceptor);
		}
		return iconn;
	}

	@Override
	public void init() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryInterceptor interceptor : interceptors) {
				denied = interceptor.init(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().init();
		}
	}

	@Override
	public void setDataDir(File dataDir) {
		boolean denied = false;
		if (activated) {
			for (RepositoryInterceptor interceptor : interceptors) {
				denied = interceptor.setDataDir(getDelegate(), dataDir);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().setDataDir(dataDir);
		}
	}

	@Override
	public void shutDown() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryInterceptor interceptor : interceptors) {
				denied = interceptor.shutDown(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().shutDown();
		}
	}
}
