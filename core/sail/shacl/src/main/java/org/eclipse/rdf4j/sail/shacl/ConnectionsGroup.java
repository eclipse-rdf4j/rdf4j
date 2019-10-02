/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 *
 */
@Deprecated
@InternalUseOnly
public class ConnectionsGroup implements Closeable {

	private final ShaclSail sail;

	private final SailConnection baseConnection;
	private final SailConnection previousStateConnection;

	private final Sail addedStatements;
	private final Sail removedStatements;

	private final Stats stats;

	private final RdfsSubClassOfReasonerProvider rdfsSubClassOfReasonerProvider;

	private final ConcurrentLinkedQueue<SailConnection> connectionsToClose = new ConcurrentLinkedQueue<>();

	// used to cache Select plan nodes so that we don't query a store for the same data during the same validation step.
	private final Map<PlanNode, BufferedSplitter> selectNodeCache = new HashMap<>();

	ConnectionsGroup(ShaclSail sail, SailConnection baseConnection,
			SailConnection previousStateConnection, Sail addedStatements, Sail removedStatements,
			Stats stats, RdfsSubClassOfReasonerProvider rdfsSubClassOfReasonerProvider) {
		this.sail = sail;
		this.baseConnection = baseConnection;
		this.previousStateConnection = previousStateConnection;
		this.addedStatements = addedStatements;
		this.removedStatements = removedStatements;
		this.stats = stats;
		this.rdfsSubClassOfReasonerProvider = rdfsSubClassOfReasonerProvider;
	}

	public SailConnection getPreviousStateConnection() {
		return previousStateConnection;
	}

	public SailConnection getAddedStatements() {
		SailConnection connection = addedStatements.getConnection();
		connectionsToClose.add(connection);
		return connection;
	}

	public SailConnection getRemovedStatements() {
		SailConnection connection = removedStatements.getConnection();
		connectionsToClose.add(connection);
		return connection;
	}

	@Override
	public void close() {
		for (SailConnection sailConnection : connectionsToClose) {
			sailConnection.close();
		}
	}

	public ShaclSail getSail() {
		return sail;
	}

	public SailConnection getBaseConnection() {
		return baseConnection;
	}

	synchronized public PlanNode getCachedNodeFor(PlanNode select) {

		if (!sail.isCacheSelectNodes()) {
			return select;
		}

		BufferedSplitter bufferedSplitter = selectNodeCache.computeIfAbsent(select, BufferedSplitter::new);

		return bufferedSplitter.getPlanNode();
	}

	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		return rdfsSubClassOfReasonerProvider.getRdfsSubClassOfReasoner();
	}

	public Stats getStats() {
		return stats;
	}

	interface RdfsSubClassOfReasonerProvider {
		RdfsSubClassOfReasoner getRdfsSubClassOfReasoner();
	}
}
