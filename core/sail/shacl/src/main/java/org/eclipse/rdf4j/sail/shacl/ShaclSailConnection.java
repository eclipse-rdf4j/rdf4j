/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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
import org.eclipse.rdf4j.sail.shacl.AST.NodeShape;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.lazy.LazyValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.lazy.ValidationResultIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailConnection.class);

	private List<NodeShape> nodeShapes;

	private final SailConnection previousStateConnection;
	private final SailConnection serializableConnection;
	private final SailConnection previousStateSerializableConnection;

	Sail addedStatements;
	Sail removedStatements;

	private final HashSet<Statement> addedStatementsSet = new HashSet<>();
	private final HashSet<Statement> removedStatementsSet = new HashSet<>();

	private boolean isShapeRefreshNeeded = false;
	private boolean shapesModifiedInCurrentTransaction = false;

	public final ShaclSail sail;

	private Stats stats;

	RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	private boolean preparedHasRun = false;

	private final SailRepositoryConnection shapesRepoConnection;

	// write lock
	private Lock writeLock;

	// used to determine if we are currently registered as a connection listener (getting added/removed notifications)
	private boolean connectionListenerActive = false;

	private IsolationLevel currentIsolationLevel = null;

	private Settings transactionSettings;
	private TransactionSetting[] transactionSettingsRaw = new TransactionSetting[0];

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection,
			SailConnection previousStateConnection, SailConnection serializableConnection,
			SailConnection previousStateSerializableConnection,
			SailRepositoryConnection shapesRepoConnection) {
		super(connection);
		this.previousStateConnection = previousStateConnection;
		this.serializableConnection = serializableConnection;
		this.previousStateSerializableConnection = previousStateSerializableConnection;
		this.shapesRepoConnection = shapesRepoConnection;
		this.sail = sail;
		this.transactionSettings = getDefaultSettings(sail);
	}

	private Settings getDefaultSettings(ShaclSail sail) {
		return new Settings(sail.isCacheSelectNodes(), sail.isValidationEnabled());
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

		currentIsolationLevel = level;

		transactionSettings = getDefaultSettings(sail);
		Arrays.stream(transactionSettingsRaw)
				.filter(Objects::nonNull)
				.forEach(setting -> {
					if (setting instanceof ShaclSail.TransactionSettings.ValidationApproach) {
						transactionSettings.validationApproach = (ShaclSail.TransactionSettings.ValidationApproach) setting;
					}
				});

		assert addedStatements == null;
		assert removedStatements == null;

		stats = new Stats();

		// start two transactions, synchronize on underlying sail so that we get two transactions immediately
		// successively
		synchronized (sail) {
			super.begin(level);
			hasStatement(null, null, null, false); // actually force a transaction to start
			shapesRepoConnection.begin(currentIsolationLevel);
			previousStateConnection.begin(currentIsolationLevel);
			previousStateConnection.hasStatement(null, null, null, false); // actually force a transaction to start
		}

		stats.setBaseSailEmpty(isEmpty());

		if (transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Disabled ||
				transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk) {
			removeConnectionListener(this);
		} else if (stats.isBaseSailEmpty()) {
			removeConnectionListener(this);
		} else {
			addConnectionListener(this);
		}

	}

	@Override
	public void addConnectionListener(SailConnectionListener listener) {
		if (!connectionListenerActive && isValidationEnabled()) {
			super.addConnectionListener(this);
			connectionListenerActive = true;
		}
	}

	boolean isValidationEnabled() {
		return transactionSettings.getValidationApproach() != ShaclSail.TransactionSettings.ValidationApproach.Disabled;
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

		if (!preparedHasRun) {
			prepare();
		}

		long before = 0;
		if (sail.isPerformanceLogging()) {
			before = System.currentTimeMillis();
		}
		previousStateConnection.commit();

		super.commit();
		shapesRepoConnection.commit();

		if (shapesModifiedInCurrentTransaction) {
			sail.setNodeShapes(nodeShapes);
		}

		if (writeLock != null && writeLock.isActive()) {
			writeLock = sail.releaseExclusiveWriteLock(writeLock);
		}

		assert writeLock == null;

		if (sail.isPerformanceLogging()) {
			logger.info("commit() excluding validation and cleanup took {} ms", System.currentTimeMillis() - before);
		}
		cleanup();
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLock = sail.acquireExclusiveWriteLock(writeLock);
			shapesRepoConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.addStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLock = sail.acquireExclusiveWriteLock(writeLock);
			shapesRepoConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLock = sail.acquireExclusiveWriteLock(writeLock);
			shapesRepoConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLock = sail.acquireExclusiveWriteLock(writeLock);
			shapesRepoConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.removeStatements(subj, pred, obj, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts) throws SailException {
		if (Arrays.asList(contexts).contains(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesRepoConnection.clear();
			isShapeRefreshNeeded = true;
		}
		super.clear(contexts);
	}

	@Override
	public void rollback() throws SailException {

		previousStateConnection.rollback();
		shapesRepoConnection.rollback();
		super.rollback();
		if (shapesModifiedInCurrentTransaction || isShapeRefreshNeeded) {
			isShapeRefreshNeeded = true; // force refresh shapes after rollback of the shapesRepoConnection
			refreshShapes();
			if (shapesModifiedInCurrentTransaction) {
				sail.setNodeShapes(nodeShapes);
			}
		}
		if ((writeLock != null && writeLock.isActive())) {
			writeLock = sail.releaseExclusiveWriteLock(writeLock);
		}
		assert writeLock == null;
		cleanup();
	}

	private void cleanup() {
		long before = 0;
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
		preparedHasRun = false;
		isShapeRefreshNeeded = false;
		shapesModifiedInCurrentTransaction = false;

		assert writeLock == null;
		currentIsolationLevel = null;
		if (sail.isPerformanceLogging()) {
			logger.info("cleanup() took {} ms", System.currentTimeMillis() - before);
		}

	}

	private void refreshShapes() {
		if (isShapeRefreshNeeded) {
			nodeShapes = sail.refreshShapes(shapesRepoConnection);
			isShapeRefreshNeeded = false;
			shapesModifiedInCurrentTransaction = true;
		}

	}

	private ValidationReport validate(List<NodeShape> nodeShapes, boolean validateEntireBaseSail) {

		try {
			if (!isValidationEnabled()) {
				return new ValidationReport(true);
			}

			try (ConnectionsGroup connectionsGroup = getConnectionsGroup()) {
				return performValidation(nodeShapes, validateEntireBaseSail, connectionsGroup);
			}
		} finally {
			rdfsSubClassOfReasoner = null;

		}
	}

	private void prepareValidation() {

		if (!isValidationEnabled()) {
			return;
		}

		if (sail.isRdfsSubClassReasoning()) {
			rdfsSubClassOfReasoner = RdfsSubClassOfReasoner.createReasoner(this);
		}

		fillAddedAndRemovedStatementRepositories();

	}

	ConnectionsGroup getConnectionsGroup() {

		return new ConnectionsGroup(new VerySimpleRdfsBackwardsChainingConnection(this, rdfsSubClassOfReasoner),
				previousStateConnection, addedStatements, removedStatements, stats,
				this::getRdfsSubClassOfReasoner, transactionSettings);
	}

	private ValidationReport performValidation(List<NodeShape> nodeShapes, boolean validateEntireBaseSail,
			ConnectionsGroup connectionsGroup) {
		long beforeValidation = 0;

		if (sail.isPerformanceLogging()) {
			beforeValidation = System.currentTimeMillis();
		}

		try {
			Stream<Callable<ValidationResultIterator>> callableStream = nodeShapes
					.stream()
					.flatMap(nodeShape -> nodeShape
							.generatePlans(connectionsGroup, nodeShape, sail.isLogValidationPlans(),
									validateEntireBaseSail))
					.filter(Objects::nonNull)
					.map(planNode -> () -> {
						ValidationExecutionLogger validationExecutionLogger = new ValidationExecutionLogger();
						planNode.receiveLogger(validationExecutionLogger);

						try (CloseableIteration<Tuple, SailException> iterator = planNode.iterator()) {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
								logger.info("Start execution of plan " + propertyShape.getNodeShape().toString() + " : "
										+ propertyShape.toString());
							}

							long before = 0;
							if (sail.isPerformanceLogging()) {
								before = System.currentTimeMillis();
							}

							ValidationResultIterator validationResults = new ValidationResultIterator(iterator,
									sail.getEffectiveValidationResultsLimitPerConstraint());

							validationExecutionLogger.flush();

							if (sail.isPerformanceLogging()) {
								long after = System.currentTimeMillis();
								PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
								logger.info("Execution of plan took {} ms for {} : {}", (after - before),
										propertyShape.getNodeShape().toString(), propertyShape.toString());
							}

							if (GlobalValidationExecutionLogging.loggingEnabled) {
								PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
								logger.info("Finished execution of plan {} : {}",
										propertyShape.getNodeShape().toString(),
										propertyShape.toString());
							}

							if (sail.isLogValidationViolations()) {
								List<Tuple> tuples = validationResults.getTuples();
								if (!validationResults.conforms()) {

									PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();

									logger.info(
											"SHACL not valid. The following experimental debug results were produced: \n\tNodeShape: {}\n\tPropertyShape: {} \n\t\t{}",
											propertyShape.getNodeShape().getId(), propertyShape.getId(),
											tuples.stream()
													.map(a -> a.toString() + " -cause-> " + a.getCause())
													.collect(Collectors.joining("\n\t\t")));
								}
							}

							return validationResults;
						}
					});

			List<ValidationResultIterator> validationResultIterators;

			if (isParallelValidation()) {

				validationResultIterators = callableStream
						.map(this.sail::submitRunnableToExecutorService)
						// Creating a list is needed to actually make things run multi-threaded, without this the
						// laziness of java streams will make this run serially
						.collect(Collectors.toList())
						.stream()
						.map(f -> {
							try {
								return f.get();
							} catch (InterruptedException | ExecutionException e) {
								throw new RuntimeException(e);
							}
						})
						.collect(Collectors.toList());

			} else {
				validationResultIterators = callableStream.map(c -> {
					try {
						return c.call();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}).collect(Collectors.toList());

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
		// bulk validation should use little memory so should not run validation in parallel
		return sail.isParallelValidation() && !isBulkValidation();
	}

	void fillAddedAndRemovedStatementRepositories() {

		long before = 0;
		if (sail.isPerformanceLogging()) {
			before = System.currentTimeMillis();
		}

		if (stats.isBaseSailEmpty()) {

			flush();

			if ((rdfsSubClassOfReasoner == null || rdfsSubClassOfReasoner.isEmpty())
					&& sail.getBaseSail() instanceof MemoryStore && this.getIsolationLevel() == IsolationLevels.NONE) {
				addedStatements = sail.getBaseSail();
				removedStatements = getNewMemorySail();
			} else {
				addedStatements = getNewMemorySail();
				removedStatements = getNewMemorySail();

				try (Stream<? extends Statement> stream = getStatements(null, null, null, false).stream()) {
					try (SailConnection connection = addedStatements.getConnection()) {
						connection.begin(IsolationLevels.NONE);
						stream
								.flatMap(statement -> rdfsSubClassOfReasoner == null ? Stream.of(statement)
										: rdfsSubClassOfReasoner.forwardChain(statement))
								.forEach(statement -> connection.addStatement(statement.getSubject(),
										statement.getPredicate(), statement.getObject(), statement.getContext()));
						connection.commit();
					}
				}
			}

		} else {

			Stream.of(addedStatementsSet, removedStatementsSet)
					.parallel()
					.forEach(set -> {
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
									.forEach(statement -> connection.addStatement(statement.getSubject(),
											statement.getPredicate(), statement.getObject(), statement.getContext()));
							connection.commit();
						}

					});

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
		try {
			if (isActive()) {
				rollback();
			}
			shapesRepoConnection.close();
			previousStateConnection.close();
			serializableConnection.close();
			previousStateSerializableConnection.close();
			super.close();
		} finally {
			sail.closeConnection(this);
		}

		assert writeLock == null;

	}

	@Override
	public void prepare() throws SailException {
		flush();

		Lock readLock = null;

		try {
			long before = 0;
			if (sail.isPerformanceLogging()) {
				before = System.currentTimeMillis();
			}

			boolean useSerializableValidation = sail.isSerializableValidation() &&
					currentIsolationLevel == IsolationLevels.SNAPSHOT &&
					!isBulkValidation() &&
					isValidationEnabled();

			if (useSerializableValidation) {
				if (!(writeLock != null && writeLock.isActive())) {
					writeLock = sail.acquireExclusiveWriteLock(writeLock);
				}
			} else {
				if (!(writeLock != null && writeLock.isActive())) {
					readLock = sail.acquireReadLock();
				}
			}

			loadCachedNodeShapes();
			List<NodeShape> nodeShapesBeforeRefresh = this.nodeShapes;

			refreshShapes();

			List<NodeShape> nodeShapesAfterRefresh = this.nodeShapes;

			stats.setEmpty(isEmpty());

			if (connectionListenerActive && addedStatementsSet.isEmpty() && removedStatementsSet.isEmpty()
					&& !shapesModifiedInCurrentTransaction) {
				if (!(stats.isBaseSailEmpty() && !stats.isEmpty())) {
					logger.debug("Nothing has changed, nothing to validate.");
					return;
				}
			}

			if (shapesModifiedInCurrentTransaction && addedStatementsSet.isEmpty() && removedStatementsSet.isEmpty()
					&& !isBulkValidation()) {
				// we can optimize which shapes to revalidate since no data has changed.
				assert nodeShapesBeforeRefresh != nodeShapesAfterRefresh;

				HashSet<NodeShape> nodeShapesBeforeRefreshSet = new HashSet<>(nodeShapesBeforeRefresh);

				nodeShapesAfterRefresh = nodeShapesAfterRefresh.stream()
						.filter(nodeShape -> !nodeShapesBeforeRefreshSet.contains(nodeShape))
						.collect(Collectors.toList());

			}

			prepareValidation();

			ValidationReport invalidTuples = null;
			if (useSerializableValidation) {
				synchronized (sail) {

					if (!sail.usesSingleConnection()) {
						invalidTuples = serializableValidation(nodeShapesAfterRefresh);
					}

				}
			}

			if (invalidTuples == null) {
//				if (writeLock != null && writeLock.isActive()) {
// also check if write lock was acquired in prepare() because if it was acquire in one of the other places then we shouldn't downgrade now.
				// also - are there actually any cases that would execute this code while using multiple threads?
//					assert readLock == null;
//					readLock = sail.convertToReadLock(writeLock);
//					writeLock = null;
//				}

				invalidTuples = validate(nodeShapesAfterRefresh,
						shapesModifiedInCurrentTransaction || isBulkValidation());
			}

			boolean valid = invalidTuples.conforms();

			if (sail.isPerformanceLogging()) {
				logger.info("prepare() including validation excluding locking and super.prepare() took {} ms",
						System.currentTimeMillis() - before);
			}

			if (!valid) {
				throw new ShaclSailValidationException(invalidTuples);
			}

		} finally {

			if (readLock != null) {
				readLock = sail.releaseReadLock(readLock);
			}
			assert readLock == null;

			preparedHasRun = true;

			previousStateConnection.prepare();
			super.prepare();
		}

	}

	private boolean isBulkValidation() {
		return transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk;
	}

	private ValidationReport serializableValidation(List<NodeShape> nodeShapesAfterRefresh) {
		try {
			try {
				try (ConnectionsGroup connectionsGroup = new ConnectionsGroup(
						new VerySimpleRdfsBackwardsChainingConnection(serializableConnection, rdfsSubClassOfReasoner),
						previousStateSerializableConnection, addedStatements, removedStatements, stats,
						this::getRdfsSubClassOfReasoner, transactionSettings)) {

					connectionsGroup.getBaseConnection().begin(IsolationLevels.SNAPSHOT);
					// actually force a transaction to start
					connectionsGroup.getBaseConnection().hasStatement(null, null, null, false);

					connectionsGroup.getPreviousStateConnection().begin(IsolationLevels.SNAPSHOT);
					// actually force a transaction to start
					connectionsGroup.getPreviousStateConnection().hasStatement(null, null, null, false);

					stats.setBaseSailEmpty(ConnectionHelper.isEmpty(connectionsGroup.getBaseConnection()));

					try (SailConnection connection = addedStatements.getConnection()) {
						SailConnection baseConnection = connectionsGroup.getBaseConnection();
						ConnectionHelper.transferStatements(connection, baseConnection::addStatement);
					}

					try (SailConnection connection = removedStatements.getConnection()) {
						SailConnection baseConnection = connectionsGroup.getBaseConnection();
						ConnectionHelper.transferStatements(connection, baseConnection::removeStatements);

					}

					serializableConnection.flush();

					return performValidation(nodeShapesAfterRefresh,
							shapesModifiedInCurrentTransaction || isBulkValidation(), connectionsGroup);

				} finally {
					serializableConnection.rollback();
				}
			} finally {
				previousStateSerializableConnection.rollback();
			}
		} finally {
			rdfsSubClassOfReasoner = null;

		}
	}

	private void loadCachedNodeShapes() {
		this.nodeShapes = sail.getNodeShapes();
	}

	@Override
	public void statementAdded(Statement statement) {
		if (preparedHasRun) {
			throw new IllegalStateException("Detected changes after prepare() has been called.");
		}
		boolean add = addedStatementsSet.add(statement);
		if (!add) {
			removedStatementsSet.remove(statement);
		}

	}

	@Override
	public void statementRemoved(Statement statement) {
		if (preparedHasRun) {
			throw new IllegalStateException("Detected changes after prepare() has been called.");
		}

		boolean add = removedStatementsSet.add(statement);
		if (!add) {
			addedStatementsSet.remove(statement);
		}
	}

	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		return rdfsSubClassOfReasoner;
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			return ConnectionHelper
					.getCloseableIteration(shapesRepoConnection.getStatements(subj, pred, obj, includeInferred));
		}

		return super.getStatements(subj, pred, obj, includeInferred, contexts);

	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {

		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			return shapesRepoConnection.hasStatement(subj, pred, obj, includeInferred);
		}

		return super.hasStatement(subj, pred, obj, includeInferred, contexts);

	}

	private boolean isEmpty() {
		return ConnectionHelper.isEmpty(this);
	}

	public ValidationReport revalidate() {

		if (!isActive()) {
			throw new IllegalStateException("No active transaction!");
		}

		loadCachedNodeShapes();
		prepareValidation();
		ValidationReport validate = validate(this.nodeShapes, true);

		return new ShaclSailValidationException(validate).getValidationReport();
	}

	public static class Settings {

		private ShaclSail.TransactionSettings.ValidationApproach validationApproach = ShaclSail.TransactionSettings.ValidationApproach.Auto;
		private boolean cacheSelectedNodes = false;

		public Settings(boolean cacheSelectNodes, boolean validationEnabled) {
			this.cacheSelectedNodes = cacheSelectNodes;
			if (!validationEnabled)
				validationApproach = ShaclSail.TransactionSettings.ValidationApproach.Disabled;
		}

		public ShaclSail.TransactionSettings.ValidationApproach getValidationApproach() {
			return validationApproach;
		}

		public boolean isCacheSelectNodes() {
			return cacheSelectedNodes && validationApproach != ShaclSail.TransactionSettings.ValidationApproach.Bulk;
		}

	}

}
