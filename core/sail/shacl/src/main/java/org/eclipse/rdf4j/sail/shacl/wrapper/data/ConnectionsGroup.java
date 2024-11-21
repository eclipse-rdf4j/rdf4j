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
import java.util.concurrent.ExecutionException;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @apiNote since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *          warning from one release to the next.
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

	private final Cache<Value, Value> INTERNED_VALUE_CACHE = CacheBuilder.newBuilder()
			.concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
			.maximumSize(10000)
			.build();

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

	public enum StatementPosition {
		subject,
		predicate,
		object
	}

	/**
	 * This method is a performance optimization for converting a more general value object, like RDF.TYPE, to the
	 * specific Value object that the underlying sail would use for that node. It uses a cache to avoid querying the
	 * store for the same value multiple times during the same validation.
	 *
	 * @param value             the value object to be converted
	 * @param statementPosition the position of the statement (subject, predicate, or object)
	 * @param connection        the SailConnection used to retrieve the specific Value object
	 * @param <T>               the type of the value
	 * @return the specific Value object used by the underlying sail, or the original value if no specific Value is
	 *         found
	 * @throws SailException if an error occurs while retrieving the specific Value object
	 */
	public <T extends Value> T getSailSpecificValue(T value, StatementPosition statementPosition,
			SailConnection connection) {
		try {

			Value t = INTERNED_VALUE_CACHE.get(value, () -> {

				switch (statementPosition) {
				case subject:
					try (var statements = connection.getStatements(((Resource) value), null, null, false).stream()) {
						Resource ret = statements.map(Statement::getSubject).findAny().orElse(null);
						if (ret == null) {
							return value;
						}
						return ret;
					}
				case predicate:
					try (var statements = connection.getStatements(null, ((IRI) value), null, false).stream()) {
						IRI ret = statements.map(Statement::getPredicate).findAny().orElse(null);
						if (ret == null) {
							return value;
						}
						return ret;
					}
				case object:
					try (var statements = connection.getStatements(null, null, value, false).stream()) {
						Value ret = statements.map(Statement::getObject).findAny().orElse(null);
						if (ret == null) {
							return value;
						}
						return ret;
					}
				}

				throw new IllegalStateException("Unknown statement position: " + statementPosition);

			});
			return ((T) t);
		} catch (ExecutionException e) {
			throw new SailException(e);
		}
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
				return BufferedSplitter.getInstance(parent);
			});

			logger.debug("Found in cache: {} {}  -  {} : {}", matchedCache[0] ? " TRUE" : "FALSE",
					bufferedSplitter.getId(), planNode.getClass().getSimpleName(), planNode.getId());

			return bufferedSplitter.getPlanNode();
		} else {
			return nodeCache.computeIfAbsent(planNode, BufferedSplitter::getInstance).getPlanNode();
		}

	}

	/**
	 * Returns the RdfsSubClassOfReasoner if it is enabled. If it is not enabled this method will return null.
	 *
	 * @return RdfsSubClassOfReasoner or null
	 */
	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		if (rdfsSubClassOfReasonerProvider == null) {
			return null;
		}
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
