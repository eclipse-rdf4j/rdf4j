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
import org.eclipse.rdf4j.repository.event.NotifyingRepository;
import org.eclipse.rdf4j.repository.event.NotifyingRepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionListener;
import org.eclipse.rdf4j.repository.event.RepositoryListener;

/**
 * This notifying decorator allows listeners to register with the repository or connection and be notified when events
 * occur.
 *
 * @author James Leigh
 * @author Herko ter Horst
 * @author Arjohn Kampman
 * @see NotifyingRepositoryConnectionWrapper
 */
public class NotifyingRepositoryWrapper extends RepositoryWrapper implements NotifyingRepository {

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean activated;

	private boolean defaultReportDeltas = false;

	private final Set<RepositoryListener> listeners = new CopyOnWriteArraySet<>();

	private final Set<RepositoryConnectionListener> conListeners = new CopyOnWriteArraySet<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NotifyingRepositoryWrapper() {
		super();
	}

	public NotifyingRepositoryWrapper(Repository delegate) {
		super(delegate);
	}

	public NotifyingRepositoryWrapper(Repository delegate, boolean defaultReportDeltas) {
		this(delegate);
		setDefaultReportDeltas(defaultReportDeltas);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public boolean getDefaultReportDeltas() {
		return defaultReportDeltas;
	}

	public void setDefaultReportDeltas(boolean defaultReportDeltas) {
		this.defaultReportDeltas = defaultReportDeltas;
	}

	/**
	 * Registers a <var>RepositoryListener</var> that will receive notifications of operations that are performed on
	 * this repository.
	 */
	@Override
	public void addRepositoryListener(RepositoryListener listener) {
		listeners.add(listener);
		activated = true;
	}

	/**
	 * Removes a registered <var>RepositoryListener</var> from this repository.
	 */
	@Override
	public void removeRepositoryListener(RepositoryListener listener) {
		listeners.remove(listener);
		activated = !listeners.isEmpty();
	}

	/**
	 * Registers a <var>RepositoryConnectionListener</var> that will receive notifications of operations that are
	 * performed on any< connections that are created by this repository.
	 */
	@Override
	public void addRepositoryConnectionListener(RepositoryConnectionListener listener) {
		conListeners.add(listener);
	}

	/**
	 * Removes a registered <var>RepositoryConnectionListener</var> from this repository.
	 */
	@Override
	public void removeRepositoryConnectionListener(RepositoryConnectionListener listener) {
		conListeners.remove(listener);
	}

	@Override
	public NotifyingRepositoryConnection getConnection() throws RepositoryException {
		RepositoryConnection con = getDelegate().getConnection();
		NotifyingRepositoryConnection ncon = new NotifyingRepositoryConnectionWrapper(this, con,
				getDefaultReportDeltas());

		if (activated) {
			for (RepositoryListener listener : listeners) {
				listener.getConnection(this, ncon);
			}
		}
		for (RepositoryConnectionListener l : conListeners) {
			ncon.addRepositoryConnectionListener(l);
		}

		return ncon;
	}

	@Override
	public void init() throws RepositoryException {
		super.init();

		if (activated) {
			for (RepositoryListener listener : listeners) {
				listener.init(this);
			}
		}
	}

	@Override
	public void setDataDir(File dataDir) {
		super.setDataDir(dataDir);

		if (activated) {
			for (RepositoryListener listener : listeners) {
				listener.setDataDir(this, dataDir);
			}
		}
	}

	@Override
	public void shutDown() throws RepositoryException {
		super.shutDown();

		if (activated) {
			for (RepositoryListener listener : listeners) {
				listener.shutDown(this);
			}
		}
	}
}
