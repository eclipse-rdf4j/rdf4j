/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.StampedLockManager;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShape;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.lazy.LazyValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.lazy.ValidationResultIterator;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.VerySimpleRdfsBackwardsChainingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailConnection.class);

	private final SailConnection previousStateConnection;
	private final SailConnection serializableConnection;

	private final boolean useDefaultShapesGraph;
	private IRI[] shapesGraphs;

	Sail addedStatements;
	Sail removedStatements;
	Sail addedStatementsInferred;
	Sail removedStatementsInferred;
	Sail addedStatementsRdfsInferred;
	Sail removedStatementsRdfsInferred;
	Sail addedStatementsWithInferred;
	Sail removedStatementsWithInferred;
	Sail addedStatementsWithRdfsInferred;
	Sail removedStatementsWithRdfsInferred;
	Sail addedStatementsWithInferredAndRdfs;
	Sail removedStatementsWithInferredAndRdfs;

	private final HashSet<Statement> addedStatementsSet = new HashSet<>();
	private final HashSet<Statement> removedStatementsSet = new HashSet<>();
	private final HashSet<Statement> addedStatementsInferredSet = new HashSet<>();
	private final HashSet<Statement> removedStatementsInferredSet = new HashSet<>();

	private boolean shapeRefreshNeeded = false;
	private boolean legacyStatementAddedWithoutInferredFlagObserved = false;
	private boolean legacyStatementRemovedWithoutInferredFlagObserved = false;
	private boolean shapesModifiedInCurrentTransaction = false;

	public final ShaclSail sail;

	private Stats stats;

	RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	private boolean prepareHasBeenCalled = false;

	private Lock exclusiveSerializableValidationLock;
	private Lock nonExclusiveSerializableValidationLock;

	private StampedLockManager.Cache<List<ContextWithShape>>.WritableState writableShapesCache;
	private StampedLockManager.Cache<List<ContextWithShape>>.ReadableState readableShapesCache;

	private final SailRepositoryConnection shapesRepoConnection;

	// used to determine if we are currently registered as a connection listener (getting added/removed notifications)
	private boolean connectionListenerActive = false;

	private IsolationLevel currentIsolationLevel = null;

	private Settings transactionSettings;
	private TransactionSetting[] transactionSettingsRaw = new TransactionSetting[0];
	private volatile boolean closed;

	private volatile List<Future<ValidationResultIterator>> futures;

	private List<ShapeValidationContainer> shapeValidatorContainers;

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection, SailConnection previousStateConnection,
			SailRepositoryConnection shapesRepoConnection, SailConnection serializableConnection) {
		super(connection);
		this.previousStateConnection = previousStateConnection;
		this.shapesRepoConnection = shapesRepoConnection;
		this.serializableConnection = serializableConnection;
		this.sail = sail;
		this.transactionSettings = getDefaultSettings(sail);
		this.useDefaultShapesGraph = sail.getShapesGraphs().contains(RDF4J.SHACL_SHAPE_GRAPH);
	}

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection, SailConnection previousStateConnection,
			SailRepositoryConnection shapesRepoConnection) {
		super(connection);
		this.previousStateConnection = previousStateConnection;
		this.shapesRepoConnection = shapesRepoConnection;
		this.serializableConnection = null;
		this.sail = sail;
		this.transactionSettings = getDefaultSettings(sail);
		this.useDefaultShapesGraph = sail.getShapesGraphs().contains(RDF4J.SHACL_SHAPE_GRAPH);
	}

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection,
			SailRepositoryConnection shapesRepoConnection, SailConnection serializableConnection) {
		super(connection);
		this.previousStateConnection = null;
		this.shapesRepoConnection = shapesRepoConnection;
		this.serializableConnection = serializableConnection;
		this.sail = sail;
		this.transactionSettings = getDefaultSettings(sail);
		this.useDefaultShapesGraph = sail.getShapesGraphs().contains(RDF4J.SHACL_SHAPE_GRAPH);
	}

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection,
			SailRepositoryConnection shapesRepoConnection) {
		super(connection);
		this.previousStateConnection = null;
		this.serializableConnection = null;
		this.shapesRepoConnection = shapesRepoConnection;
		this.sail = sail;
		this.transactionSettings = getDefaultSettings(sail);
		this.useDefaultShapesGraph = sail.getShapesGraphs().contains(RDF4J.SHACL_SHAPE_GRAPH);
	}

	private Settings getDefaultSettings(ShaclSail sail) {
		return new Settings(sail.isCacheSelectNodes(), sail.isValidationEnabled(), sail.isParallelValidation(),
				currentIsolationLevel);
	}

	@Override
	public void setTransactionSettings(TransactionSetting... settings) {
		super.setTransactionSettings(settings);
		this.transactionSettingsRaw = settings;
	}

	@Override
	public void begin() throws SailException {
		begin(sail.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		if (closed) {
			throw new SailException("Connection is closed");
		}
		if (sail.isShutdown()) {
			throw new SailException("Sail is shutdown");
		}

		shapeValidatorContainers = new ArrayList<>();
		currentIsolationLevel = level;

		assert addedStatements == null;
		assert removedStatements == null;
		assert readableShapesCache == null;
		assert writableShapesCache == null;
		assert nonExclusiveSerializableValidationLock == null;
		assert exclusiveSerializableValidationLock == null;
		assert shapesGraphs == null;

		shapesGraphs = sail.getShapesGraphs().stream().map(g -> {
			if (g.equals(RDF4J.NIL)) {
				return null;
			}
			if (g.equals(SESAME.NIL)) {
				return null;
			}
			return g;
		}).toArray(IRI[]::new);

		stats = new Stats();

		// start two transactions, synchronize on underlying sail so that we get two transactions immediately
		// successively
		synchronized (sail) {
			super.begin(level);
			hasStatement(null, null, null, false); // actually force a transaction to start
			shapesRepoConnection.begin(currentIsolationLevel);
			if (previousStateConnection != null) {
				previousStateConnection.begin(currentIsolationLevel);
				previousStateConnection.hasStatement(null, null, null, false); // actually force a transaction to start
			}
		}

		stats.setEmptyBeforeTransaction(ConnectionHelper.isEmpty(this));

		transactionSettings = getDefaultSettings(sail);

		if (stats.wasEmptyBeforeTransaction() && !shouldUseSerializableValidation()) {
			transactionSettings.switchToBulkValidation();
		}

		transactionSettings.applyTransactionSettings(getLocalTransactionSettings());

		assert transactionSettings.parallelValidation != null;
		assert transactionSettings.cacheSelectedNodes != null;
		assert transactionSettings.validationApproach != null;

		if (isBulkValidation() || !isValidationEnabled()) {
			removeConnectionListener(this);
		} else {
			addConnectionListener(this);
		}

	}

	/**
	 * @return the transaction settings that are based purely on the settings based down through the begin(...) method
	 *         without considering any sail level settings for things like caching or parallel validation.
	 */
	private Settings getLocalTransactionSettings() {
		return new Settings(this);
	}

	@Override
	public void addConnectionListener(SailConnectionListener listener) {
		if (!connectionListenerActive && isValidationEnabled()) {
			super.addConnectionListener(this);
			connectionListenerActive = true;
		}
	}

	boolean isValidationEnabled() {
		return transactionSettings.getValidationApproach() != ValidationApproach.Disabled;
	}

	@Override
	public void removeConnectionListener(SailConnectionListener listener) {
		super.removeConnectionListener(listener);
		connectionListenerActive = false;

	}

	private Sail getNewMemorySail() {
		MemoryStore sail = new MemoryStore();
		sail.setDefaultIsolationLevel(IsolationLevels.NONE);
		sail.init();
		return sail;
	}

	@Override
	public void commit() throws SailException {
		if (closed) {
			throw new SailException("Connection is closed");
		}

		if (!prepareHasBeenCalled) {
			prepare();
		}

		try {
			long before = getTimeStamp();
			if (previousStateConnection != null) {
				previousStateConnection.commit();
			}
			super.commit();
			shapesRepoConnection.commit();

			if (sail.isPerformanceLogging()) {
				logger.info("commit() excluding validation and cleanup took {} ms", getTimeStamp() - before);
			}
		} finally {
			cleanup();
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.add(subj, pred, obj, contexts);
			markShapesRefreshNeeded();
		} else {
			super.addStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.remove(subj, pred, obj, contexts);
			markShapesRefreshNeeded();
		} else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.add(subj, pred, obj, contexts);
			markShapesRefreshNeeded();
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.remove(subj, pred, obj, contexts);
			markShapesRefreshNeeded();
		} else {
			super.removeStatements(subj, pred, obj, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts) throws SailException {
		if (Arrays.asList(contexts).contains(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesRepoConnection.clear();
			markShapesRefreshNeeded();
		}
		super.clear(contexts);
	}

	@Override
	public void rollback() throws SailException {
		if (closed) {
			throw new SailException("Connection is closed");
		}

		try {
			if (readableShapesCache != null) {
				try {
					readableShapesCache.close();
				} finally {
					readableShapesCache = null;
				}
			}
		} finally {
			try {
				if (writableShapesCache != null) {
					try {
						writableShapesCache.purge();
					} finally {
						try {
							writableShapesCache.close();
						} finally {
							writableShapesCache = null;
						}
					}
				}
			} finally {
				try {
					if (previousStateConnection != null && previousStateConnection.isActive()) {
						previousStateConnection.rollback();
					}
				} finally {
					try {
						if (serializableConnection != null && serializableConnection.isActive()) {
							serializableConnection.rollback();
						}
					} finally {
						try {
							if (shapesRepoConnection.isActive()) {
								shapesRepoConnection.rollback();
							}
						} finally {
							try {
								if (isActive()) {
									super.rollback();
								}
							} finally {
								cleanup();
							}
						}
					}
				}
			}
		}
	}

	private void cleanup() {
		long before = 0;

		try {
			if (sail.isPerformanceLogging()) {
				before = System.currentTimeMillis();
			}

			logger.debug("Cleanup");

			if (addedStatements != null) {
				if (addedStatements != sail.getBaseSail()) {
					addedStatements.shutDown();
				}
				addedStatements = null;
			}

			if (removedStatements != null) {
				removedStatements.shutDown();
				removedStatements = null;
			}
			if (addedStatementsInferred != null) {
				addedStatementsInferred.shutDown();
				addedStatementsInferred = null;
			}
			if (removedStatementsInferred != null) {
				removedStatementsInferred.shutDown();
				removedStatementsInferred = null;
			}
			if (addedStatementsRdfsInferred != null) {
				addedStatementsRdfsInferred.shutDown();
				addedStatementsRdfsInferred = null;
			}
			if (removedStatementsRdfsInferred != null) {
				removedStatementsRdfsInferred.shutDown();
				removedStatementsRdfsInferred = null;
			}
			if (addedStatementsWithInferred != null) {
				addedStatementsWithInferred.shutDown();
				addedStatementsWithInferred = null;
			}
			if (removedStatementsWithInferred != null) {
				removedStatementsWithInferred.shutDown();
				removedStatementsWithInferred = null;
			}
			if (addedStatementsWithRdfsInferred != null) {
				addedStatementsWithRdfsInferred.shutDown();
				addedStatementsWithRdfsInferred = null;
			}
			if (removedStatementsWithRdfsInferred != null) {
				removedStatementsWithRdfsInferred.shutDown();
				removedStatementsWithRdfsInferred = null;
			}
			if (addedStatementsWithInferredAndRdfs != null) {
				addedStatementsWithInferredAndRdfs.shutDown();
				addedStatementsWithInferredAndRdfs = null;
			}
			if (removedStatementsWithInferredAndRdfs != null) {
				removedStatementsWithInferredAndRdfs.shutDown();
				removedStatementsWithInferredAndRdfs = null;
			}

			addedStatementsSet.clear();
			removedStatementsSet.clear();
			addedStatementsInferredSet.clear();
			removedStatementsInferredSet.clear();
			stats = null;
			prepareHasBeenCalled = false;
			shapeRefreshNeeded = false;
			legacyStatementAddedWithoutInferredFlagObserved = false;
			legacyStatementRemovedWithoutInferredFlagObserved = false;
			shapesModifiedInCurrentTransaction = false;

			currentIsolationLevel = null;

			shapesGraphs = null;

		} finally {
			try {
				cleanupShapesReadWriteLock();
			} finally {
				cleanupReadWriteLock();
			}

			if (sail.isPerformanceLogging()) {
				logger.info("cleanup() took {} ms", System.currentTimeMillis() - before);
			}
		}

	}

	private void cleanupShapesReadWriteLock() {
		try {
			if (writableShapesCache != null) {
				try {
					// we need to refresh the shapes cache!
					writableShapesCache.purge();
				} finally {
					writableShapesCache.close();
				}
			}
		} finally {
			if (readableShapesCache != null) {
				readableShapesCache.close();
			}
		}

		writableShapesCache = null;
		readableShapesCache = null;

	}

	private void cleanupReadWriteLock() {
		try {
			if (exclusiveSerializableValidationLock != null) {
				exclusiveSerializableValidationLock.release();
			}
		} finally {
			if (nonExclusiveSerializableValidationLock != null) {
				nonExclusiveSerializableValidationLock.release();
			}
		}

		exclusiveSerializableValidationLock = null;
		nonExclusiveSerializableValidationLock = null;

	}

	private ValidationReport validate(List<ContextWithShape> shapes, boolean validateEntireBaseSail)
			throws InterruptedException {

		assert isValidationEnabled();

		try {
			try (ConnectionsGroup connectionsGroup = getConnectionsGroup()) {
				return performValidation(shapes, validateEntireBaseSail, connectionsGroup, this,
						previousStateConnection);
			}
		} finally {
			rdfsSubClassOfReasoner = null;
		}

	}

	void prepareValidation(ValidationSettings validationSettings, boolean requireRdfsSubClassReasoning)
			throws InterruptedException {

		assert isValidationEnabled();

		if (requireRdfsSubClassReasoning) {
			rdfsSubClassOfReasoner = RdfsSubClassOfReasoner.createReasoner(this, validationSettings);
		} else {
			rdfsSubClassOfReasoner = null;
		}

		if (sail.isShutdown()) {
			throw new SailException("Sail is shutdown");
		}

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}

		if (!isBulkValidation()) {
			fillAddedAndRemovedStatementRepositories();
		}

	}

	ConnectionsGroup getConnectionsGroup() {
		return getConnectionsGroup(this, previousStateConnection, sail.isIncludeInferredStatements(),
				sail.isRdfsSubClassReasoning());
	}

	ConnectionsGroup getConnectionsGroup(SailConnection baseConnection, SailConnection previousStateConnection,
			boolean includeInferredStatements, boolean useRdfsSubClassReasoning) {
		RdfsSubClassOfReasoner reasoner = useRdfsSubClassReasoning ? rdfsSubClassOfReasoner : null;
		ConnectionsGroup.RdfsSubClassOfReasonerProvider provider = reasoner == null ? null : () -> reasoner;
		Sail effectiveAddedStatements = getEffectiveAddedStatements(includeInferredStatements,
				useRdfsSubClassReasoning);
		Sail effectiveRemovedStatements = getEffectiveRemovedStatements(includeInferredStatements,
				useRdfsSubClassReasoning);

		return new ConnectionsGroup(
				new VerySimpleRdfsBackwardsChainingConnection(baseConnection, reasoner, includeInferredStatements),
				previousStateConnection, effectiveAddedStatements, effectiveRemovedStatements, stats,
				provider, includeInferredStatements, transactionSettings, sail.sparqlValidation);
	}

	private Sail getEffectiveAddedStatements(boolean includeInferredStatements, boolean useRdfsSubClassReasoning) {
		boolean includeBaseInferred = includeInferredStatements && addedStatementsInferred != null;
		boolean includeRdfsInferred = useRdfsSubClassReasoning && addedStatementsRdfsInferred != null;
		if (!includeBaseInferred && !includeRdfsInferred) {
			return addedStatements;
		}
		if (includeBaseInferred && includeRdfsInferred) {
			if (addedStatementsWithInferredAndRdfs == null) {
				addedStatementsWithInferredAndRdfs = buildCombinedStatements(addedStatements,
						addedStatementsInferred, addedStatementsRdfsInferred);
			}
			return addedStatementsWithInferredAndRdfs;
		}
		if (includeBaseInferred) {
			if (addedStatementsWithInferred == null) {
				addedStatementsWithInferred = buildCombinedStatements(addedStatements, addedStatementsInferred, null);
			}
			return addedStatementsWithInferred;
		}
		if (addedStatementsWithRdfsInferred == null) {
			addedStatementsWithRdfsInferred = buildCombinedStatements(addedStatements, null,
					addedStatementsRdfsInferred);
		}
		return addedStatementsWithRdfsInferred;
	}

	private Sail getEffectiveRemovedStatements(boolean includeInferredStatements, boolean useRdfsSubClassReasoning) {
		boolean includeBaseInferred = includeInferredStatements && removedStatementsInferred != null;
		boolean includeRdfsInferred = useRdfsSubClassReasoning && removedStatementsRdfsInferred != null;
		if (!includeBaseInferred && !includeRdfsInferred) {
			return removedStatements;
		}
		if (includeBaseInferred && includeRdfsInferred) {
			if (removedStatementsWithInferredAndRdfs == null) {
				removedStatementsWithInferredAndRdfs = buildCombinedStatements(removedStatements,
						removedStatementsInferred, removedStatementsRdfsInferred);
			}
			return removedStatementsWithInferredAndRdfs;
		}
		if (includeBaseInferred) {
			if (removedStatementsWithInferred == null) {
				removedStatementsWithInferred = buildCombinedStatements(removedStatements, removedStatementsInferred,
						null);
			}
			return removedStatementsWithInferred;
		}
		if (removedStatementsWithRdfsInferred == null) {
			removedStatementsWithRdfsInferred = buildCombinedStatements(removedStatements, null,
					removedStatementsRdfsInferred);
		}
		return removedStatementsWithRdfsInferred;
	}

	private Sail buildCombinedStatements(Sail explicitStatements, Sail inferredStatements,
			Sail rdfsInferredStatements) {
		if (explicitStatements == null && inferredStatements == null && rdfsInferredStatements == null) {
			return null;
		}
		Sail combinedStatements = getNewMemorySail();
		try (SailConnection combinedConnection = combinedStatements.getConnection()) {
			combinedConnection.begin(IsolationLevels.NONE);
			copyStatements(explicitStatements, combinedConnection);
			copyStatements(inferredStatements, combinedConnection);
			copyStatements(rdfsInferredStatements, combinedConnection);
			combinedConnection.commit();
		}
		return combinedStatements;
	}

	private void copyStatements(Sail source, SailConnection target) {
		if (source == null) {
			return;
		}
		try (SailConnection from = source.getConnection()) {
			ConnectionHelper.transferStatements(from, target::addStatement);
		}
	}

	private void resetCombinedStatementStores() {
		if (addedStatementsWithInferred != null) {
			addedStatementsWithInferred.shutDown();
			addedStatementsWithInferred = null;
		}
		if (removedStatementsWithInferred != null) {
			removedStatementsWithInferred.shutDown();
			removedStatementsWithInferred = null;
		}
		if (addedStatementsWithRdfsInferred != null) {
			addedStatementsWithRdfsInferred.shutDown();
			addedStatementsWithRdfsInferred = null;
		}
		if (removedStatementsWithRdfsInferred != null) {
			removedStatementsWithRdfsInferred.shutDown();
			removedStatementsWithRdfsInferred = null;
		}
		if (addedStatementsWithInferredAndRdfs != null) {
			addedStatementsWithInferredAndRdfs.shutDown();
			addedStatementsWithInferredAndRdfs = null;
		}
		if (removedStatementsWithInferredAndRdfs != null) {
			removedStatementsWithInferredAndRdfs.shutDown();
			removedStatementsWithInferredAndRdfs = null;
		}
	}

	private boolean requiresRdfsSubClassReasoner(List<ContextWithShape> shapes) {
		return shapes.stream()
				.map(ContextWithShape::getShape)
				.map(Shape::getRdfsSubClassReasoningOverride)
				.anyMatch(Boolean.TRUE::equals);
	}

	private ValidationReport performValidation(List<ContextWithShape> shapes, boolean validateEntireBaseSail,
			ConnectionsGroup connectionsGroup, SailConnection baseConnection, SailConnection previousStateConnection)
			throws InterruptedException {
		long beforeValidation = 0;
		boolean defaultIncludeInferredStatements = sail.isIncludeInferredStatements();
		boolean defaultRdfsSubClassReasoning = sail.isRdfsSubClassReasoning();

		if (sail.isPerformanceLogging()) {
			beforeValidation = System.currentTimeMillis();
		}

		try {
			int numberOfShapes = shapes.size();

			Stream<Callable<ValidationResultIterator>> callableStream = shapes
					.stream()
					.map(contextWithShapes -> {
						Shape shape = contextWithShapes.getShape();
						boolean shapeRdfsSubClassReasoning = shape
								.usesRdfsSubClassReasoning(defaultRdfsSubClassReasoning);
						boolean shapeIncludeInferredStatements = shape
								.usesIncludeInferredStatements(defaultIncludeInferredStatements);

						boolean closeConnectionsGroup = false;
						ConnectionsGroup shapeConnectionsGroup = connectionsGroup;
						if (shapeRdfsSubClassReasoning != defaultRdfsSubClassReasoning
								|| shapeIncludeInferredStatements != defaultIncludeInferredStatements) {
							shapeConnectionsGroup = getConnectionsGroup(baseConnection, previousStateConnection,
									shapeIncludeInferredStatements, shapeRdfsSubClassReasoning);
							closeConnectionsGroup = true;
						}
						ConnectionsGroup planConnectionsGroup = shapeConnectionsGroup;

						return new ShapeValidationContainer(
								shape,
								() -> shape.generatePlans(planConnectionsGroup,
										new ValidationSettings(contextWithShapes.getDataGraph(),
												sail.isLogValidationPlans(), validateEntireBaseSail,
												sail.isPerformanceLogging())),
								sail.isGlobalLogValidationExecution(), sail.isLogValidationViolations(),
								sail.getEffectiveValidationResultsLimitPerConstraint(), sail.isPerformanceLogging(),
								sail.isLogValidationPlans(),
								logger,
								shapeConnectionsGroup,
								closeConnectionsGroup);
					})

					.filter(ShapeValidationContainer::hasPlanNode)
					.peek(s -> {
						if (sail.isShutdown()) {
							throw new SailException("Sail is shutdown");
						}
						if (closed) {
							throw new SailException("Connection is closed");
						}
						synchronized (shapeValidatorContainers) {
							try {
								if (closed) {
									try {
										s.forceClose();
									} catch (Throwable ignored) {
										logger.debug("Throwable was ignored while closing connection", ignored);
									}
									throw new SailException("Connection is closed");
								}
								shapeValidatorContainers.add(s);
							} catch (Throwable t) {
								try {
									s.forceClose();
								} catch (Throwable ignored) {
									logger.debug("Throwable was ignored while closing connection", ignored);
								}
								if (closed) {
									throw new SailException("Connection is closed", t);
								}
								throw t;
							}
						}
					})
					.map(validationContainer -> validationContainer::performValidation);

			List<ValidationResultIterator> validationResultIterators = new ArrayList<>(numberOfShapes);

			futures = new ArrayList<Future<ValidationResultIterator>>(numberOfShapes);

			boolean parallelValidation = numberOfShapes > 1 && isParallelValidation();

			try {
				callableStream
						.map(callable -> {
							if (Thread.currentThread().isInterrupted()) {
								return null;
							}

							if (sail.isShutdown()) {
								throw new SailException("Sail is shutdown");
							}
							if (closed) {
								throw new SailException("Connection is closed");
							}

							if (parallelValidation) {
								try {
									return sail.submitToExecutorService(callable);
								} catch (Throwable e) {
									if (sail.isShutdown()) {
										throw new SailException("Sail is shutdown", e);
									}
									if (closed) {
										throw new SailException("Connection is closed", e);
									}
									throw e;
								}
							} else {
								FutureTask<ValidationResultIterator> futureTask = new FutureTask<>(callable);
								futureTask.run();
								return futureTask;
							}
						})
						.filter(Objects::nonNull)
						.forEach(f -> {
							synchronized (futures) {
								try {
									if (closed) {
										f.cancel(true);
										throw new SailException("Connection is closed");
									}
									futures.add(f);
								} catch (Throwable t) {
									f.cancel(true);
									if (closed) {
										throw new SailException("Connection is closed", t);
									}
									throw t;
								}
							}
						});

				boolean done = false;

				ArrayDeque<Future<ValidationResultIterator>> futures1;
				synchronized (futures) {
					futures1 = new ArrayDeque<>(futures);
				}

				while (!futures1.isEmpty()) {
					Future<ValidationResultIterator> future = futures1.removeFirst();

					assert future != null;
					try {
						if (sail.isShutdown()) {
							throw new SailException("Sail is shutdown");
						}
						if (closed) {
							throw new SailException("Connection is closed");
						}
						if (!Thread.currentThread().isInterrupted()) {
							ValidationResultIterator validationResultIterator = future.get(100, TimeUnit.MILLISECONDS);
							validationResultIterators.add(validationResultIterator);
						}
					} catch (CancellationException e) {
						Thread.currentThread().interrupt();
						InterruptedException interruptedException = new InterruptedException(
								"Validation future was cancelled");
						interruptedException.initCause(e);
						throw interruptedException;
					} catch (ExecutionException e) {
						Throwable cause = e.getCause();
						if (cause instanceof InterruptedException) {
							throw new InterruptedException();
						} else if (cause instanceof RuntimeException) {
							throw ((RuntimeException) cause);
						} else if (cause instanceof Error) {
							throw ((Error) cause);
						} else {
							// this should only happen if we throw a checked exception from the Callable that
							// isn't handled in the if/elseif above
							assert false;
							throw new IllegalStateException(cause);
						}
					} catch (TimeoutException e) {
						futures1.addLast(future);
					}

				}

				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
			} finally {
				var originalFutures = this.futures;
				synchronized (originalFutures) {
					for (Future<ValidationResultIterator> future : originalFutures) {
						future.cancel(true);
					}
					this.futures = List.of();
				}

			}

			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}

			return new LazyValidationReport(validationResultIterators, sail.getValidationResultsLimitTotal());

		} finally {
			if (sail.isPerformanceLogging()) {
				logger.info("Actual validation and generating plans took {} ms",
						System.currentTimeMillis() - beforeValidation);
			}
		}
	}

	private boolean isParallelValidation() {
		assert !(transactionSettings.isParallelValidation() && !supportsConcurrentReads());
		assert !(getIsolationLevel() == IsolationLevels.SERIALIZABLE && transactionSettings
				.isParallelValidation()) : "Concurrent reads is buggy for SERIALIZABLE transactions.";

		return transactionSettings.isParallelValidation();
	}

	void fillAddedAndRemovedStatementRepositories() throws InterruptedException {

		assert !isBulkValidation();
		assert isValidationEnabled();

		long before = 0;
		if (sail.isPerformanceLogging()) {
			before = System.currentTimeMillis();
		}

		List<Future<Object>> futures = new ArrayList<>();

		boolean parallelValidation = isParallelValidation() && !addedStatementsSet.isEmpty()
				&& !removedStatementsSet.isEmpty();

		resetCombinedStatementStores();

		try {
			Stream.of(addedStatementsSet, removedStatementsSet)
					.map(set -> (Callable<Object>) () -> {

						Set<Statement> otherSet;
						Sail explicitRepository;
						Sail inferredRepository = null;
						if (set == addedStatementsSet) {
							otherSet = removedStatementsSet;

							if (addedStatements != null && addedStatements != sail.getBaseSail()) {
								addedStatements.shutDown();
							}

							addedStatements = getNewMemorySail();
							explicitRepository = addedStatements;
							if (rdfsSubClassOfReasoner != null) {
								if (addedStatementsRdfsInferred != null) {
									addedStatementsRdfsInferred.shutDown();
								}
								addedStatementsRdfsInferred = getNewMemorySail();
								inferredRepository = addedStatementsRdfsInferred;
							} else if (addedStatementsRdfsInferred != null) {
								addedStatementsRdfsInferred.shutDown();
								addedStatementsRdfsInferred = null;
							}

							set.forEach(stats::added);

						} else {
							otherSet = addedStatementsSet;

							if (removedStatements != null) {
								removedStatements.shutDown();
								removedStatements = null;
							}

							removedStatements = getNewMemorySail();
							explicitRepository = removedStatements;
							if (rdfsSubClassOfReasoner != null) {
								if (removedStatementsRdfsInferred != null) {
									removedStatementsRdfsInferred.shutDown();
								}
								removedStatementsRdfsInferred = getNewMemorySail();
								inferredRepository = removedStatementsRdfsInferred;
							} else if (removedStatementsRdfsInferred != null) {
								removedStatementsRdfsInferred.shutDown();
								removedStatementsRdfsInferred = null;
							}

							set.forEach(stats::removed);
						}

						try (SailConnection explicitConnection = explicitRepository.getConnection();
								SailConnection inferredConnection = inferredRepository != null
										? inferredRepository.getConnection()
										: null) {
							explicitConnection.begin(IsolationLevels.NONE);
							if (inferredConnection != null) {
								inferredConnection.begin(IsolationLevels.NONE);
							}
							set.stream()
									.peek(s -> {
										if (Thread.currentThread().isInterrupted()) {
											throw new SailException(
													"ShacilSailConnection was interrupted while filling added/removed statement repositories");
										}
									})
									.filter(statement -> !otherSet.contains(statement))
									.forEach(statement -> {
										if (!Thread.currentThread().isInterrupted()) {
											explicitConnection.addStatement(statement.getSubject(),
													statement.getPredicate(), statement.getObject(),
													statement.getContext());
											if (inferredConnection != null) {
												rdfsSubClassOfReasoner.forwardChain(statement)
														.forEach(inferredStatement -> inferredConnection
																.addStatement(inferredStatement.getSubject(),
																		inferredStatement.getPredicate(),
																		inferredStatement.getObject(),
																		inferredStatement.getContext()));
											}
										}

									});
							if (Thread.interrupted()) {
								throw new InterruptedException();
							}

							if (inferredConnection != null) {
								inferredConnection.commit();
							}
							explicitConnection.commit();
						}

						return null;

					})
					.map(callable -> {
						if (Thread.currentThread().isInterrupted()) {
							return null;
						}
						if (closed) {
							throw new SailException("Connection is closed");
						}
						if (sail.isShutdown()) {
							throw new SailException("Sail is shutdown");
						}
						if (parallelValidation) {
							return sail.submitToExecutorService(callable);
						} else {
							FutureTask<Object> objectFutureTask = new FutureTask<>(callable);
							objectFutureTask.run();
							return objectFutureTask;
						}
					})
					.filter(Objects::nonNull)
					.forEach(futures::add);

			for (Future<Object> future : futures) {
				try {
					if (!Thread.currentThread().isInterrupted()) {
						future.get();
					}
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (cause instanceof InterruptedException) {
						throw ((InterruptedException) cause);
					} else if (cause instanceof RuntimeException) {
						throw ((RuntimeException) cause);
					} else if (cause instanceof Error) {
						throw ((Error) cause);
					} else {
						// this should only happen if we throw a checked exception from the Callable that isn't handled
						// in the if/elseif above
						throw new IllegalStateException(cause);
					}
				}
			}

			fillInferredStatementRepository(addedStatementsInferredSet, removedStatementsInferredSet, true);
			fillInferredStatementRepository(removedStatementsInferredSet, addedStatementsInferredSet, false);

		} finally {
			if (futures != null) {
				for (Future<Object> future : futures) {
					future.cancel(true);
				}
			}
		}

		if (sail.isPerformanceLogging()) {
			logger.info("fillAddedAndRemovedStatementRepositories() took {} ms", System.currentTimeMillis() - before);
		}

	}

	private void fillInferredStatementRepository(Set<Statement> sourceSet, Set<Statement> otherSet,
			boolean added) throws InterruptedException {
		if (sourceSet.isEmpty()) {
			if (added) {
				if (addedStatementsInferred != null) {
					addedStatementsInferred.shutDown();
					addedStatementsInferred = null;
				}
			} else {
				if (removedStatementsInferred != null) {
					removedStatementsInferred.shutDown();
					removedStatementsInferred = null;
				}
			}
			return;
		}

		Sail inferredRepository;
		if (added) {
			if (addedStatementsInferred != null) {
				addedStatementsInferred.shutDown();
			}
			addedStatementsInferred = getNewMemorySail();
			inferredRepository = addedStatementsInferred;
			sourceSet.forEach(stats::added);
		} else {
			if (removedStatementsInferred != null) {
				removedStatementsInferred.shutDown();
			}
			removedStatementsInferred = getNewMemorySail();
			inferredRepository = removedStatementsInferred;
			sourceSet.forEach(stats::removed);
		}

		try (SailConnection inferredConnection = inferredRepository.getConnection()) {
			inferredConnection.begin(IsolationLevels.NONE);
			sourceSet.stream()
					.peek(s -> {
						if (Thread.currentThread().isInterrupted()) {
							throw new SailException(
									"ShacilSailConnection was interrupted while filling inferred statement repositories");
						}
					})
					.filter(statement -> !otherSet.contains(statement))
					.forEach(statement -> {
						if (!Thread.currentThread().isInterrupted()) {
							inferredConnection.addStatement(statement.getSubject(),
									statement.getPredicate(), statement.getObject(), statement.getContext());
						}
					});
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			inferredConnection.commit();
		}
	}

	private IsolationLevel getIsolationLevel() {
		return currentIsolationLevel;
	}

	@Override
	synchronized public void close() throws SailException {
		if (closed) {
			return;
		}

		closed = true;

		try {
			var originalFutures = this.futures;
			if (originalFutures != null) {
				synchronized (originalFutures) {
					for (Future<ValidationResultIterator> future : futures) {
						future.cancel(true);
					}
					this.futures = List.of();
				}
			}
		} finally {
			try {
				var originalShapeValidatorContainers = this.shapeValidatorContainers;
				if (originalShapeValidatorContainers != null) {
					synchronized (originalShapeValidatorContainers) {
						for (ShapeValidationContainer shapeValidatorContainer : originalShapeValidatorContainers) {
							try {
								shapeValidatorContainer.forceClose();
							} catch (Throwable ignored) {
								logger.debug("Throwable was ignored while closing connection", ignored);
							}
						}
						shapeValidatorContainers = List.of();
					}
				}
			} finally {
				try {
					waitForOperations();
				} finally {
					try {
						if (readableShapesCache != null) {
							readableShapesCache.close();
							readableShapesCache = null;
						}
					} finally {
						try {
							if (writableShapesCache != null) {
								try {
									writableShapesCache.purge();
								} finally {
									writableShapesCache.close();
									writableShapesCache = null;
								}
							}
						} finally {
							innerClose();
						}
					}
				}
			}
		}
	}

	private void innerClose() {
		try {
			shapesRepoConnection.close();

		} finally {
			try {
				if (previousStateConnection != null) {
					previousStateConnection.close();
				}

			} finally {
				try {
					if (serializableConnection != null) {
						serializableConnection.close();
					}
				} finally {

					try {
						super.close();
					} finally {
						try {
							sail.closeConnection();
						} finally {
							try {
								cleanupShapesReadWriteLock();
							} finally {
								try {
									cleanupReadWriteLock();
								} finally {
									cleanup();
								}
							}
						}
					}
				}
			}
		}
	}

	private void waitForOperations() {
		if (getWrappedConnection() instanceof AbstractSailConnection) {
			AbstractSailConnection abstractSailConnection = (AbstractSailConnection) getWrappedConnection();

			abstractSailConnection.waitForOtherOperations(true);
			for (int i = 0; i < 50 && abstractSailConnection.hasActiveIterations(); i++) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	@Override
	public void prepare() throws SailException {
		if (closed) {
			throw new SailException("Connection is closed");
		}

		if (sail.isShutdown()) {
			throw new SailException("Sail is shutdown");
		}

		prepareHasBeenCalled = true;

		long before = 0;
		flush();

		try {

			if (sail.isPerformanceLogging()) {
				before = System.currentTimeMillis();
			}

			boolean useSerializableValidation = shouldUseSerializableValidation() && !isBulkValidation();

			if (sail.isSerializableValidation()) {
				if (useSerializableValidation) {
					exclusiveSerializableValidationLock = sail.serializableValidationLock.getWriteLock();
				} else {
					nonExclusiveSerializableValidationLock = sail.serializableValidationLock.getReadLock();
				}
			} else {
				assert !useSerializableValidation
						: "ShaclSail does not have serializable validation enabled but ShaclSailConnection still attempted to use serializable validation!";
			}

			if (!isValidationEnabled()) {
				logger.debug("Validation skipped because validation was disabled");
				if (shapeRefreshNeeded || !connectionListenerActive) {
					// getting the shapes write lock will ensure that the shapes cache is refreshed when cleanup() is
					// called after commit/rollback
					writableShapesCache = sail.getCachedShapesForWriting();
				}
				return;
			}

			assert !shapeRefreshNeeded
					|| !shapesModifiedInCurrentTransaction
					: "isShapeRefreshNeeded should trigger shapesModifiedInCurrentTransaction once we have loaded the modified shapes, but shapesModifiedInCurrentTransaction should be null until then";

			if (!shapeRefreshNeeded && !isBulkValidation() && addedStatementsSet.isEmpty()
					&& removedStatementsSet.isEmpty()) {
				logger.debug("Nothing has changed, nothing to validate.");
				return;
			}

			List<ContextWithShape> currentShapes = null;
			List<ContextWithShape> shapesAfterRefresh = null;

			if (shapeRefreshNeeded || !connectionListenerActive || isBulkValidation()) {
				if (writableShapesCache == null) {
					writableShapesCache = sail.getCachedShapesForWriting();
				}

				shapesModifiedInCurrentTransaction = shapeRefreshNeeded;
				shapeRefreshNeeded = false;
				shapesAfterRefresh = sail.getShapes(shapesRepoConnection, this, shapesGraphs);
			} else {
				if (readableShapesCache == null) {
					readableShapesCache = sail.getCachedShapes();
				}
			}

			if (readableShapesCache != null) {
				currentShapes = readableShapesCache.getData();
			}

			assert currentShapes != null || shapesAfterRefresh != null;
			assert !(currentShapes != null && shapesAfterRefresh != null);

			if (isEmpty(currentShapes) && isEmpty(shapesAfterRefresh)) {
				logger.debug("Validation skipped because there are no shapes to validate");
				return;
			}

			List<ContextWithShape> shapesToValidate = shapesAfterRefresh != null ? shapesAfterRefresh : currentShapes;
			validateLegacyCallbackInferredSupport(shapesToValidate);
			boolean requiresRdfsSubClassReasoner = sail.isRdfsSubClassReasoning()
					|| requiresRdfsSubClassReasoner(shapesToValidate);

			stats.setEmptyIncludingCurrentTransaction(ConnectionHelper.isEmpty(this));

			prepareValidation(
					new ValidationSettings(null, sail.isLogValidationPlans(), false, sail.isPerformanceLogging()),
					requiresRdfsSubClassReasoner);

			ValidationReport invalidTuples = null;
			if (useSerializableValidation) {
				synchronized (sail.singleConnectionMonitor) {
					if (!sail.usesSingleConnection()) {
						invalidTuples = serializableValidation(shapesToValidate);
					}
				}
			}

			if (invalidTuples == null) {
				invalidTuples = validate(
						shapesToValidate,
						shapesModifiedInCurrentTransaction || isBulkValidation());
			}

			boolean valid = invalidTuples.conforms();

			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("ShaclSailConnection was interrupted while validating.");
			}

			if (closed) {
				throw new SailException("Connection is closed");
			}

			if (!valid) {
				throw new ShaclSailValidationException(invalidTuples);
			}

		} catch (InterruptedException e) {
			throw ShaclSail.convertToSailException(e);
		} finally {

			if (sail.isPerformanceLogging()) {
				logger.info("prepare() including validation (excluding flushing and super.prepare()) took {} ms",
						System.currentTimeMillis() - before);
			}

			// if the thread has been interrupted we should try to return quickly
			if (!Thread.currentThread().isInterrupted()) {
				shapesRepoConnection.prepare();
				if (previousStateConnection != null) {
					previousStateConnection.prepare();
				}
				super.prepare();
			}

		}

	}

	private boolean isEmpty(List<ContextWithShape> shapesList) {
		if (shapesList == null) {
			return true;
		}
		for (ContextWithShape shapesWithContext : shapesList) {
			if (shapesWithContext.hasShape()) {
				return false;
			}
		}
		return true;
	}

	private boolean shouldUseSerializableValidation() {
		return serializableConnection != null && sail.isSerializableValidation()
				&& currentIsolationLevel == IsolationLevels.SNAPSHOT;
	}

	private boolean isBulkValidation() {
		return transactionSettings.getValidationApproach() == ValidationApproach.Bulk;
	}

	private ValidationReport serializableValidation(List<ContextWithShape> shapesAfterRefresh)
			throws InterruptedException {
		try {
			try (ConnectionsGroup connectionsGroup = getConnectionsGroup(serializableConnection, null,
					sail.isIncludeInferredStatements(), sail.isRdfsSubClassReasoning())) {

				connectionsGroup.getBaseConnection().begin(IsolationLevels.SNAPSHOT);
				// actually force a transaction to start
				connectionsGroup.getBaseConnection().hasStatement(null, null, null, false);

				stats.setEmptyBeforeTransaction(ConnectionHelper.isEmpty(connectionsGroup.getBaseConnection()));

				try (SailConnection connection = addedStatements.getConnection()) {
					SailConnection baseConnection = connectionsGroup.getBaseConnection();
					ConnectionHelper.transferStatements(connection, baseConnection::addStatement);
				}

				try (SailConnection connection = removedStatements.getConnection()) {
					SailConnection baseConnection = connectionsGroup.getBaseConnection();
					ConnectionHelper.transferStatements(connection, baseConnection::removeStatements);
				}

				serializableConnection.flush();

				return performValidation(shapesAfterRefresh, shapesModifiedInCurrentTransaction || isBulkValidation(),
						connectionsGroup, serializableConnection, null);

			} finally {
				serializableConnection.rollback();
			}

		} finally {
			rdfsSubClassOfReasoner = null;

		}
	}

	@Override
	public void statementAdded(Statement statement) {
		legacyStatementAddedWithoutInferredFlagObserved = true;
		statementAdded(statement, false);
	}

	@Override
	public void statementAdded(Statement statement, boolean inferred) {
		if (prepareHasBeenCalled) {
			throw new IllegalStateException("Detected changes after prepare() has been called.");
		}
		checkIfShapesRefreshIsNeeded(statement);
		if (inferred) {
			boolean add = addedStatementsInferredSet.add(statement);
			if (!add) {
				removedStatementsInferredSet.remove(statement);
			}
		} else {
			boolean add = addedStatementsSet.add(statement);
			if (!add) {
				removedStatementsSet.remove(statement);
			}
		}

		checkTransactionalValidationLimit();

	}

	@Override
	public void statementRemoved(Statement statement) {
		legacyStatementRemovedWithoutInferredFlagObserved = true;
		statementRemoved(statement, false);
	}

	@Override
	public void statementRemoved(Statement statement, boolean inferred) {
		if (prepareHasBeenCalled) {
			throw new IllegalStateException("Detected changes after prepare() has been called.");
		}
		checkIfShapesRefreshIsNeeded(statement);

		if (inferred) {
			boolean add = removedStatementsInferredSet.add(statement);
			if (!add) {
				addedStatementsInferredSet.remove(statement);
			}
		} else {
			boolean add = removedStatementsSet.add(statement);
			if (!add) {
				addedStatementsSet.remove(statement);
			}
		}

		checkTransactionalValidationLimit();
	}

	private void checkIfShapesRefreshIsNeeded(Statement statement) {

		if (!shapeRefreshNeeded) {
			for (IRI shapesGraph : shapesGraphs) {
				if (Objects.equals(statement.getContext(), shapesGraph)) {
					markShapesRefreshNeeded();
					break;
				}
			}
		}
	}

	private void markShapesRefreshNeeded() {
		shapeRefreshNeeded = true;
	}

	private Boolean inferInferredFromStatementMetadata(Statement statement) {
		try {
			Boolean inferred = invokeBooleanStatementMethod(statement, "isInferred");
			if (inferred != null) {
				return inferred;
			}
			Boolean explicit = invokeBooleanStatementMethod(statement, "isExplicit");
			if (explicit != null) {
				return !explicit;
			}
			return null;
		} catch (ReflectiveOperationException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to infer inferred-flag from legacy callback statement metadata.", e);
			}
			return null;
		}
	}

	private Boolean invokeBooleanStatementMethod(Statement statement, String methodName)
			throws ReflectiveOperationException {
		var method = statement.getClass().getMethod(methodName);
		Class<?> returnType = method.getReturnType();
		if (returnType != boolean.class && returnType != Boolean.class) {
			return null;
		}
		Object value = method.invoke(statement);
		return value == null ? null : (Boolean) value;
	}

	/**
	 * Reject legacy no-flag callbacks only when a shape explicitly disables inferred statements using
	 * rsx:includeInferredStatements=false. In that case, inferred-vs-explicit classification is required for correct
	 * validation and stores must use statementAdded/Removed callbacks with the inferred boolean argument.
	 */
	private void validateLegacyCallbackInferredSupport(List<ContextWithShape> shapesToValidate) {
		if (!legacyStatementAddedWithoutInferredFlagObserved && !legacyStatementRemovedWithoutInferredFlagObserved) {
			return;
		}

		boolean hasExplicitIncludeInferredDisabled = shapesToValidate.stream()
				.filter(ContextWithShape::hasShape)
				.map(ContextWithShape::getShape)
				.map(Shape::getIncludeInferredStatementsOverride)
				.anyMatch(Boolean.FALSE::equals);

		if (!hasExplicitIncludeInferredDisabled) {
			return;
		}

		String callbackDetails = getObservedLegacyCallbacksWithoutInferredFlag();
		String message = "Underlying Sail does not support shapes that explicitly set "
				+ "rsx:includeInferredStatements=false because it emits deprecated "
				+ "SailConnectionListener callbacks without inferred flags (" + callbackDetails + "). "
				+ "Implement statementAdded(Statement, boolean inferred) and "
				+ "statementRemoved(Statement, boolean inferred).";
		logger.error(message);
		throw new ShaclSailValidationException(message);
	}

	private String getObservedLegacyCallbacksWithoutInferredFlag() {
		if (legacyStatementAddedWithoutInferredFlagObserved && legacyStatementRemovedWithoutInferredFlagObserved) {
			return "statementAdded(Statement), statementRemoved(Statement)";
		}
		if (legacyStatementAddedWithoutInferredFlagObserved) {
			return "statementAdded(Statement)";
		}
		if (legacyStatementRemovedWithoutInferredFlagObserved) {
			return "statementRemoved(Statement)";
		}
		return "<none>";
	}

	private void checkTransactionalValidationLimit() {
		int changeCount = addedStatementsSet.size() + removedStatementsSet.size()
				+ addedStatementsInferredSet.size() + removedStatementsInferredSet.size();
		if (changeCount > sail.getTransactionalValidationLimit()) {
			if (shouldUseSerializableValidation()) {
				logger.debug(
						"Transaction size limit exceeded, could not switch to bulk validation because serializable validation is enabled.");
			} else {
				logger.debug("Transaction size limit exceeded, reverting to bulk validation.");
				removeConnectionListener(this);
				Settings bulkValidation = getLocalTransactionSettings();
				bulkValidation.setValidationApproach(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
				getTransactionSettings().applyTransactionSettings(bulkValidation);
				removedStatementsSet.clear();
				addedStatementsSet.clear();
				removedStatementsInferredSet.clear();
				addedStatementsInferredSet.clear();
			}
		}
	}

	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		return rdfsSubClassOfReasoner;
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			return ConnectionHelper
					.getCloseableIteration(shapesRepoConnection.getStatements(subj, pred, obj, includeInferred));
		}

		return super.getStatements(subj, pred, obj, includeInferred, contexts);

	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {

		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			return shapesRepoConnection.hasStatement(subj, pred, obj, includeInferred);
		}

		return super.hasStatement(subj, pred, obj, includeInferred, contexts);

	}

	public ValidationReport revalidate() {

		if (!isActive()) {
			throw new IllegalStateException("No active transaction!");
		}
		try {
			return validate(sail.getShapes(shapesRepoConnection, this, shapesGraphs), true);
		} catch (InterruptedException e) {
			throw ShaclSail.convertToSailException(e);
		}
	}

	Settings getTransactionSettings() {
		return transactionSettings;
	}

	private long getTimeStamp() {
		if (sail.isPerformanceLogging()) {
			return System.currentTimeMillis();
		}
		return 0;
	}

	public static class Settings {

		private ValidationApproach validationApproach;
		private Boolean cacheSelectedNodes;
		private Boolean parallelValidation;
		private IsolationLevel isolationLevel;
		transient private Settings previous = null;

		public Settings(boolean cacheSelectNodes, boolean validationEnabled, boolean parallelValidation,
				IsolationLevel isolationLevel) {
			this.cacheSelectedNodes = cacheSelectNodes;
			if (!validationEnabled) {
				validationApproach = ValidationApproach.Disabled;
			} else {
				this.validationApproach = ValidationApproach.Auto;
			}
			this.parallelValidation = parallelValidation;
			this.isolationLevel = isolationLevel;
		}

		public Settings(ShaclSailConnection connection) {

			TransactionSetting[] transactionSettingsRaw = connection.transactionSettingsRaw;
			assert transactionSettingsRaw != null;

			ValidationApproach validationApproach = null;
			Boolean cacheSelectedNodes = null;
			Boolean parallelValidation = null;

			for (TransactionSetting transactionSetting : transactionSettingsRaw) {
				if (transactionSetting instanceof ValidationApproach) {
					validationApproach = (ValidationApproach) transactionSetting;
				} else if (transactionSetting instanceof ShaclSail.TransactionSettings.PerformanceHint) {
					switch (((ShaclSail.TransactionSettings.PerformanceHint) transactionSetting)) {
					case ParallelValidation:
						parallelValidation = true;
						break;
					case SerialValidation:
						parallelValidation = false;
						break;
					case CacheDisabled:
						cacheSelectedNodes = false;
						break;
					case CacheEnabled:
						cacheSelectedNodes = true;
						break;
					}

				}
			}

			this.validationApproach = validationApproach;
			this.cacheSelectedNodes = cacheSelectedNodes;

			if (!connection.supportsConcurrentReads()) {
				this.parallelValidation = false;
			} else {
				this.parallelValidation = parallelValidation;
			}
		}

		private Settings(Settings settings) {
			this.validationApproach = settings.validationApproach;
			this.cacheSelectedNodes = settings.cacheSelectedNodes;
			this.parallelValidation = settings.parallelValidation;
			this.isolationLevel = settings.isolationLevel;
			this.previous = settings.previous;
		}

		public ValidationApproach getValidationApproach() {
			return validationApproach;
		}

		public boolean isCacheSelectNodes() {
			return cacheSelectedNodes;
		}

		public boolean isParallelValidation() {
			return parallelValidation;
		}

		public IsolationLevel getIsolationLevel() {
			return isolationLevel;
		}

		static ValidationApproach getMostSignificantValidationApproach(
				ValidationApproach base,
				ValidationApproach overriding) {
			if (base == null && overriding == null) {
				return ValidationApproach.Auto;
			}

			return ValidationApproach.getHighestPriority(base, overriding);

		}

		void applyTransactionSettings(Settings transactionSettingsLocal) {

			previous = new Settings(this);

			// get the most significant validation approach first (eg. if validation is disabled on the sail level, then
			// validation can not be enabled on the transaction level
			validationApproach = getMostSignificantValidationApproach(validationApproach,
					transactionSettingsLocal.validationApproach);

			// apply restrictions first
			if (validationApproach == ValidationApproach.Bulk) {
				cacheSelectedNodes = false;
				parallelValidation = false;
			}

			// override settings
			if (transactionSettingsLocal.parallelValidation != null) {
				parallelValidation = transactionSettingsLocal.parallelValidation;
			}

			if (transactionSettingsLocal.cacheSelectedNodes != null) {
				cacheSelectedNodes = transactionSettingsLocal.cacheSelectedNodes;
			}

			assert transactionSettingsLocal.isolationLevel == null;

		}

		@Override
		public String toString() {
			return "Settings{" +
					"validationApproach=" + validationApproach +
					", cacheSelectedNodes=" + cacheSelectedNodes +
					", parallelValidation=" + parallelValidation +
					", isolationLevel=" + isolationLevel +
					'}';
		}

		public void switchToBulkValidation() {
			ValidationApproach newValidationApproach = getMostSignificantValidationApproach(validationApproach,
					ValidationApproach.Bulk);

			if (newValidationApproach != this.validationApproach) {
				this.validationApproach = newValidationApproach;
				parallelValidation = false;
				cacheSelectedNodes = false;
			}
		}

		private void setValidationApproach(ValidationApproach validationApproach) {
			this.validationApproach = validationApproach;
		}

		private void setCacheSelectedNodes(Boolean cacheSelectedNodes) {
			this.cacheSelectedNodes = cacheSelectedNodes;
		}

		private void setParallelValidation(Boolean parallelValidation) {
			this.parallelValidation = parallelValidation;
		}

		private void setIsolationLevel(IsolationLevel isolationLevel) {
			this.isolationLevel = isolationLevel;
		}
	}

}
