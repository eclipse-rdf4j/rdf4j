/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @apiNote since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *          warning from one release to the next.
 *
 */
@InternalUseOnly
public class ConnectionsGroup implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionsGroup.class);

	private final SailConnection baseConnection;
	private final SailConnection previousStateConnection;

	private final SailConnection addedStatements;
	private final SailConnection removedStatements;

	private final ShaclSailConnection.Settings transactionSettings;

	private final Stats stats;

	private final RdfsSubClassOfReasonerProvider rdfsSubClassOfReasonerProvider;
	private final boolean sparqlValidation;

	// used to cache Select plan nodes so that we don't query a store for the same data during the same validation step.
	private final Map<PlanNode, BufferedSplitter> nodeCache = new ConcurrentHashMap<>();

	public ConnectionsGroup(SailConnection baseConnection,
			SailConnection previousStateConnection, Sail addedStatements, Sail removedStatements,
			Stats stats, RdfsSubClassOfReasonerProvider rdfsSubClassOfReasonerProvider,
			ShaclSailConnection.Settings transactionSettings, boolean sparqlValidation) {
		this.baseConnection = baseConnection;
		this.previousStateConnection = previousStateConnection;

		this.stats = stats;
		this.rdfsSubClassOfReasonerProvider = rdfsSubClassOfReasonerProvider;
		this.transactionSettings = transactionSettings;
		this.sparqlValidation = sparqlValidation;

		if (addedStatements != null) {
			this.addedStatements = addedStatements.getConnection();
			this.addedStatements.begin(IsolationLevels.NONE);
		} else {
			this.addedStatements = null;
		}

		if (removedStatements != null) {
			this.removedStatements = removedStatements.getConnection();
			this.removedStatements.begin(IsolationLevels.NONE);
		} else {
			this.removedStatements = null;
		}
	}

	public SailConnection getPreviousStateConnection() {
		return previousStateConnection;
	}

	public boolean hasPreviousStateConnection() {
		return previousStateConnection != null;
	}

	public SailConnection getAddedStatements() {
		return addedStatements;
	}

	public SailConnection getRemovedStatements() {
		return removedStatements;
	}

	@Override
	public void close() {
		if (addedStatements != null) {
			addedStatements.commit();
			addedStatements.close();
		}
		if (removedStatements != null) {
			removedStatements.commit();
			removedStatements.close();
		}

		nodeCache.clear();
	}

	public SailConnection getBaseConnection() {
		return baseConnection;
	}

	public PlanNode getCachedNodeFor(PlanNode planNode) {

		if (!transactionSettings.isCacheSelectNodes()) {
			return planNode;
		}

		if (planNode instanceof UnorderedSelect || planNode instanceof UnBufferedPlanNode
				|| (planNode instanceof BufferedSplitter.BufferedSplitterPlaneNode
						&& ((BufferedSplitter.BufferedSplitterPlaneNode) planNode).cached)) {
			return planNode;
		}

		if (logger.isDebugEnabled()) {
			boolean[] matchedCache = { true };

			BufferedSplitter bufferedSplitter = nodeCache.computeIfAbsent(planNode, parent -> {
				matchedCache[0] = false;
				return new BufferedSplitter(parent, true, true);
			});

			logger.debug("Found in cache: {} {}  -  {} : {}", matchedCache[0] ? " TRUE" : "FALSE",
					bufferedSplitter.getId(), planNode.getClass().getSimpleName(), planNode.getId());

			return bufferedSplitter.getPlanNode();
		} else {
			return nodeCache.computeIfAbsent(planNode, BufferedSplitter::new).getPlanNode();
		}

	}

	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		return rdfsSubClassOfReasonerProvider.getRdfsSubClassOfReasoner();
	}

	public Stats getStats() {
		return stats;
	}

	public ShaclSailConnection.Settings getTransactionSettings() {
		return transactionSettings;
	}

	public boolean isSparqlValidation() {
		return sparqlValidation;
	}

	public boolean hasAddedStatements() {
		return addedStatements != null;
	}

	public interface RdfsSubClassOfReasonerProvider {
		RdfsSubClassOfReasoner getRdfsSubClassOfReasoner();
	}
}
