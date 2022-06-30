/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
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
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShapes;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SingleCloseablePlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
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

	private final HashSet<Statement> addedStatementsSet = new HashSet<>();
	private final HashSet<Statement> removedStatementsSet = new HashSet<>();

	private boolean shapeRefreshNeeded = false;
	private boolean shapesModifiedInCurrentTransaction = false;

	public final ShaclSail sail;

	private Stats stats;

	RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	private boolean prepareHasBeenCalled = false;

	private Lock exclusiveSerializableValidationLock;
	private Lock nonExclusiveSerializableValidationLock;

	private StampedLockManager.Cache<List<ContextWithShapes>>.WritableState writableShapesCache;
	private StampedLockManager.Cache<List<ContextWithShapes>>.ReadableState readableShapesCache;

	private final SailRepositoryConnection shapesRepoConnection;

	// used to determine if we are currently registered as a connection listener (getting added/removed notifications)
	private boolean connectionListenerActive = false;

	private IsolationLevel currentIsolationLevel = null;

	private Settings transactionSettings;
	private TransactionSetting[] transactionSettingsRaw = new TransactionSetting[0];
	private volatile boolean closed;

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
			shapeRefreshNeeded = true;
		} else {
			super.addStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.remove(subj, pred, obj, contexts);
			shapeRefreshNeeded = true;
		} else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.add(subj, pred, obj, contexts);
			shapeRefreshNeeded = true;
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (useDefaultShapesGraph && contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.remove(subj, pred, obj, contexts);
			shapeRefreshNeeded = true;
		} else {
			super.removeStatements(subj, pred, obj, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts) throws SailException {
		if (Arrays.asList(contexts).contains(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesRepoConnection.clear();
			shapeRefreshNeeded = true;
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
				readableShapesCache.close();
				readableShapesCache = null;
			}

			if (writableShapesCache != null) {
				writableShapesCache.purge();
				writableShapesCache.close();
				writableShapesCache = null;
			}

			if (previousStateConnection != null && previousStateConnection.isActive()) {
				previousStateConnection.rollback();
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

			addedStatementsSet.clear();
			removedStatementsSet.clear();
			stats = null;
			prepareHasBeenCalled = false;
			shapeRefreshNeeded = false;
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

	private ValidationReport validate(List<ContextWithShapes> shapes, boolean validateEntireBaseSail)
			throws InterruptedException {

		assert isValidationEnabled();

		try {
			try (ConnectionsGroup connectionsGroup = getConnectionsGroup()) {
				return performValidation(shapes, validateEntireBaseSail, connectionsGroup);
			}
		} finally {
			rdfsSubClassOfReasoner = null;
		}

	}

	void prepareValidation() throws InterruptedException {

		assert isValidationEnabled();

		if (sail.isRdfsSubClassReasoning()) {
			rdfsSubClassOfReasoner = RdfsSubClassOfReasoner.createReasoner(this);
		}

		if (!isBulkValidation()) {
			fillAddedAndRemovedStatementRepositories();
		}

	}

	ConnectionsGroup getConnectionsGroup() {

		return new ConnectionsGroup(new VerySimpleRdfsBackwardsChainingConnection(this, rdfsSubClassOfReasoner),
				previousStateConnection, addedStatements, removedStatements, stats,
				this::getRdfsSubClassOfReasoner, transactionSettings, sail.sparqlValidation);
	}

	private ValidationReport performValidation(List<ContextWithShapes> shapes, boolean validateEntireBaseSail,
			ConnectionsGroup connectionsGroup) throws InterruptedException {
		long beforeValidation = 0;

		if (sail.isPerformanceLogging()) {
			beforeValidation = System.currentTimeMillis();
		}

		try {
			Stream<Callable<ValidationResultIterator>> callableStream = shapes
					.stream()
					.flatMap(contextWithShapes -> contextWithShapes.getShapes()
							.stream()
							.map(shape -> new ValidationContainer(
									shape,
									shape.generatePlans(connectionsGroup,
											new ValidationSettings(contextWithShapes.getDataGraph(),
													sail.isLogValidationPlans(), validateEntireBaseSail,
													sail.isPerformanceLogging()))
							))
					)
					.filter(ValidationContainer::hasPlanNode)
					.map(validationContainer -> validationContainer::performValidation);

			List<ValidationResultIterator> validationResultIterators;

			List<Future<ValidationResultIterator>> futures = Collections.emptyList();

			try {
				futures = callableStream
						.map(callable -> {
							if (Thread.currentThread().isInterrupted()) {
								return null;
							}

							if (isParallelValidation()) {
								return sail.submitToExecutorService(callable);
							} else {
								FutureTask<ValidationResultIterator> futureTask = new FutureTask<>(callable);
								futureTask.run();
								return futureTask;
							}
						})
						.collect(Collectors.toList());

				validationResultIterators = futures.stream()
						.map(future -> {
							assert future != null;
							try {
								if (!Thread.currentThread().isInterrupted()) {
									return future.get();
								}
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							} catch (ExecutionException e) {
								Throwable cause = e.getCause();
								if (cause instanceof InterruptedException) {
									Thread.currentThread().interrupt();
								} else if (cause instanceof RuntimeException) {
									throw ((RuntimeException) cause);
								} else if (cause instanceof Error) {
									throw ((Error) cause);
								} else {
									// this should only happen if we throw a checked exception from the Callable that
									// isn't handled in the if/elseif above
									throw new IllegalStateException(cause);
								}
							}
							return null;
						})
						.collect(Collectors.toList());
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
			} finally {
				for (Future<ValidationResultIterator> future : futures) {
					future.cancel(true);
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

		List<Future<Object>> futures = Collections.emptyList();

		try {
			futures = Stream.of(addedStatementsSet, removedStatementsSet)
					.map(set -> (Callable<Object>) () -> {

						Set<Statement> otherSet;
						Sail repository;
						if (set == addedStatementsSet) {
							otherSet = removedStatementsSet;

							if (addedStatements != null && addedStatements != sail.getBaseSail()) {
								addedStatements.shutDown();
							}

							addedStatements = getNewMemorySail();
							repository = addedStatements;

							set.forEach(stats::added);

						} else {
							otherSet = addedStatementsSet;

							if (removedStatements != null) {
								removedStatements.shutDown();
								removedStatements = null;
							}

							removedStatements = getNewMemorySail();
							repository = removedStatements;

							set.forEach(stats::removed);
						}

						try (SailConnection connection = repository.getConnection()) {
							connection.begin(IsolationLevels.NONE);
							set.stream()
									.filter(statement -> !otherSet.contains(statement))
									.flatMap(statement -> rdfsSubClassOfReasoner == null ? Stream.of(statement)
											: rdfsSubClassOfReasoner.forwardChain(statement))
									.forEach(statement -> {
										if (!Thread.currentThread().isInterrupted()) {
											connection.addStatement(statement.getSubject(),
													statement.getPredicate(), statement.getObject(),
													statement.getContext());
										}

									});
							if (Thread.interrupted()) {
								throw new InterruptedException();
							}

							connection.commit();
						}

						return null;

					})
					.map(callable -> {
						if (Thread.currentThread().isInterrupted()) {
							return null;
						}
						if (isParallelValidation()) {
							return sail.submitToExecutorService(callable);
						} else {
							FutureTask<Object> objectFutureTask = new FutureTask<>(callable);
							objectFutureTask.run();
							return objectFutureTask;
						}
					})
					.collect(Collectors.toList());

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

		} finally {
			for (Future<Object> future : futures) {
				future.cancel(true);
			}
		}

		if (sail.isPerformanceLogging()) {
			logger.info("fillAddedAndRemovedStatementRepositories() took {} ms", System.currentTimeMillis() - before);
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

		try {
			if (isActive()) {
				rollback();
			}
		} finally {
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
										closed = true;
									}

								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void prepare() throws SailException {
		if (closed) {
			throw new SailException("Connection is closed");
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
				assert !useSerializableValidation : "ShaclSail does not have serializable validation enabled but ShaclSailConnection still attempted to use serializable validation!";
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
					|| !shapesModifiedInCurrentTransaction : "isShapeRefreshNeeded should trigger shapesModifiedInCurrentTransaction once we have loaded the modified shapes, but shapesModifiedInCurrentTransaction should be null until then";

			if (!shapeRefreshNeeded && !isBulkValidation() && addedStatementsSet.isEmpty()
					&& removedStatementsSet.isEmpty()) {
				logger.debug("Nothing has changed, nothing to validate.");
				return;
			}

			List<ContextWithShapes> currentShapes = null;
			List<ContextWithShapes> shapesAfterRefresh = null;

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

			stats.setEmptyIncludingCurrentTransaction(ConnectionHelper.isEmpty(this));

			prepareValidation();

			ValidationReport invalidTuples = null;
			if (useSerializableValidation) {
				synchronized (sail.singleConnectionMonitor) {
					if (!sail.usesSingleConnection()) {
						invalidTuples = serializableValidation(
								shapesAfterRefresh != null ? shapesAfterRefresh : currentShapes);
					}
				}
			}

			if (invalidTuples == null) {
				invalidTuples = validate(
						shapesAfterRefresh != null ? shapesAfterRefresh : currentShapes,
						shapesModifiedInCurrentTransaction || isBulkValidation());
			}

			boolean valid = invalidTuples.conforms();

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

	private boolean isEmpty(List<ContextWithShapes> shapesList) {
		if (shapesList == null) {
			return true;
		}
		for (ContextWithShapes shapesWithContext : shapesList) {
			if (!shapesWithContext.getShapes().isEmpty()) {
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

	private ValidationReport serializableValidation(List<ContextWithShapes> shapesAfterRefresh)
			throws InterruptedException {
		try {
			try (ConnectionsGroup connectionsGroup = new ConnectionsGroup(
					new VerySimpleRdfsBackwardsChainingConnection(serializableConnection, rdfsSubClassOfReasoner), null,
					addedStatements, removedStatements, stats, this::getRdfsSubClassOfReasoner, transactionSettings,
					sail.sparqlValidation)) {

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
						connectionsGroup);

			} finally {
				serializableConnection.rollback();
			}

		} finally {
			rdfsSubClassOfReasoner = null;

		}
	}

	@Override
	public void statementAdded(Statement statement) {
		if (prepareHasBeenCalled) {
			throw new IllegalStateException("Detected changes after prepare() has been called.");
		}
		checkIfShapesRefreshIsNeeded(statement);
		boolean add = addedStatementsSet.add(statement);
		if (!add) {
			removedStatementsSet.remove(statement);
		}

		checkTransactionalValidationLimit();

	}

	@Override
	public void statementRemoved(Statement statement) {
		if (prepareHasBeenCalled) {
			throw new IllegalStateException("Detected changes after prepare() has been called.");
		}
		checkIfShapesRefreshIsNeeded(statement);

		boolean add = removedStatementsSet.add(statement);
		if (!add) {
			addedStatementsSet.remove(statement);
		}

		checkTransactionalValidationLimit();
	}

	private void checkIfShapesRefreshIsNeeded(Statement statement) {

		if (!shapeRefreshNeeded) {
			for (IRI shapesGraph : shapesGraphs) {
				if (Objects.equals(statement.getContext(), shapesGraph)) {
					shapeRefreshNeeded = true;
					break;
				}
			}
		}
	}

	private void checkTransactionalValidationLimit() {
		if ((addedStatementsSet.size() + removedStatementsSet.size()) > sail.getTransactionalValidationLimit()) {
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
			}
		}
	}

	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		return rdfsSubClassOfReasoner;
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
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

	private class ValidationContainer {
		private final Shape shape;
		private final PlanNode planNode;
		private final ValidationExecutionLogger validationExecutionLogger;

		public ValidationContainer(Shape shape, PlanNode planNode) {
			this.shape = shape;
			this.validationExecutionLogger = ValidationExecutionLogger
					.getInstance(sail.isGlobalLogValidationExecution());
			if (!(planNode instanceof EmptyNode)) {
				this.planNode = new SingleCloseablePlanNode(planNode);
				this.planNode.receiveLogger(validationExecutionLogger);

			} else {
				this.planNode = planNode;
			}
		}

		public Shape getShape() {
			return shape;
		}

		public PlanNode getPlanNode() {
			return planNode;
		}

		public boolean hasPlanNode() {
			return !(planNode instanceof EmptyNode);
		}

		public ValidationResultIterator performValidation() {
			long before = getTimeStamp();

			handlePreLogging();

			ValidationResultIterator validationResults = null;

			try (CloseableIteration<? extends ValidationTuple, SailException> iterator = planNode.iterator()) {
				validationResults = new ValidationResultIterator(iterator,
						sail.getEffectiveValidationResultsLimitPerConstraint());
				return validationResults;
			} finally {
				handlePostLogging(before, validationResults);
			}
		}

		private void handlePreLogging() {
			if (validationExecutionLogger.isEnabled()) {
				logger.info("Start execution of plan:\n{}\n", getShape().toString());
			}
		}

		private void handlePostLogging(long before, ValidationResultIterator validationResults) {
			if (validationExecutionLogger.isEnabled()) {
				validationExecutionLogger.flush();
			}

			if (validationResults != null) {

				if (sail.isPerformanceLogging()) {
					long after = System.currentTimeMillis();
					logger.info("Execution of plan took {} ms for:\n{}\n",
							(after - before),
							getShape().toString());
				}

				if (validationExecutionLogger.isEnabled()) {
					logger.info("Finished execution of plan:\n{}\n",
							getShape().toString());
				}

				if (sail.isLogValidationViolations()) {
					if (!validationResults.conforms()) {
						List<ValidationTuple> tuples = validationResults.getTuples();

						logger.info(
								"SHACL not valid. The following experimental debug results were produced:  \n\t\t{}\n\n{}\n",
								tuples.stream()
										.map(ValidationTuple::toString)
										.collect(Collectors.joining("\n\t\t")),
								getShape().toString()

						);
					}
				}

			}

		}

	}

	public static class Settings {

		private ValidationApproach validationApproach;
		private Boolean cacheSelectedNodes;
		private Boolean parallelValidation;
		private IsolationLevel isolationLevel;
		transient private Settings previous = null;

		@Deprecated(since = "4.0.0", forRemoval = true)
		public Settings() {
		}

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
