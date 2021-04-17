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
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SingleCloseablePlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
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
	private Lock readLock;

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

		currentIsolationLevel = level;

		Settings localTransactionSettings = new Settings();

		Arrays.stream(transactionSettingsRaw)
				.filter(Objects::nonNull)
				.forEach(setting -> {
					if (setting instanceof ShaclSail.TransactionSettings.ValidationApproach) {
						localTransactionSettings.validationApproach = (ShaclSail.TransactionSettings.ValidationApproach) setting;
					}
					if (setting instanceof ShaclSail.TransactionSettings.PerformanceHint) {
						switch (((ShaclSail.TransactionSettings.PerformanceHint) setting)) {
						case ParallelValidation:
							localTransactionSettings.parallelValidation = true;
							break;
						case SerialValidation:
							localTransactionSettings.parallelValidation = false;
							break;
						case CacheDisabled:
							localTransactionSettings.cacheSelectedNodes = false;
							break;
						case CacheEnabled:
							localTransactionSettings.cacheSelectedNodes = true;
							break;
						}
					}
				});

		transactionSettings = getDefaultSettings(sail);
		transactionSettings.applyTransactionSettings(localTransactionSettings);

		assert transactionSettings.parallelValidation != null;
		assert transactionSettings.cacheSelectedNodes != null;
		assert transactionSettings.validationApproach != null;

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

		if (this.transactionSettings
				.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Disabled ||
				this.transactionSettings
						.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk) {
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

		if (writeLock != null && writeLock.isActive()) {
			writeLock = sail.releaseExclusiveWriteLock(writeLock);
		}

		if (readLock != null && readLock.isActive()) {
			readLock = sail.releaseReadLock(readLock);
		}

		assert writeLock == null;
		assert readLock == null;

		if (sail.isPerformanceLogging()) {
			logger.info("commit() excluding validation and cleanup took {} ms", System.currentTimeMillis() - before);
		}
		cleanup();
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
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
			shapesRepoConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesRepoConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
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

		try {
			if (previousStateConnection.isActive()) {
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
					if ((writeLock != null && writeLock.isActive())) {
						writeLock = sail.releaseExclusiveWriteLock(writeLock);
					}

					if ((readLock != null && readLock.isActive())) {
						readLock = sail.releaseReadLock(readLock);
					}

					assert writeLock == null;
					assert readLock == null;

					cleanup();
				}
			}
		}

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
		assert readLock == null;

		currentIsolationLevel = null;
		if (sail.isPerformanceLogging()) {
			logger.info("cleanup() took {} ms", System.currentTimeMillis() - before);
		}

	}

	private ValidationReport validate(List<Shape> shapes, boolean validateEntireBaseSail) {

		try {
			if (!isValidationEnabled()) {
				return new ValidationReport(true);
			}

			try (ConnectionsGroup connectionsGroup = getConnectionsGroup()) {
				return performValidation(shapes, validateEntireBaseSail, connectionsGroup);
			}
		} finally {
			rdfsSubClassOfReasoner = null;

		}
	}

	void prepareValidation() {

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

	private ValidationReport performValidation(List<Shape> shapes, boolean validateEntireBaseSail,
			ConnectionsGroup connectionsGroup) {
		long beforeValidation = 0;

		if (sail.isPerformanceLogging()) {
			beforeValidation = System.currentTimeMillis();
		}

		try {
			Stream<Callable<ValidationResultIterator>> callableStream = shapes
					.stream()
					.map(shape -> new ShapePlanNodeTuple(shape,
							shape.generatePlans(connectionsGroup, sail.isLogValidationPlans(),
									validateEntireBaseSail)))
					.filter(ShapePlanNodeTuple::hasPlanNode)
					.map(shapePlanNodeTuple -> {
						shapePlanNodeTuple.setPlanNode(new SingleCloseablePlanNode(shapePlanNodeTuple.getPlanNode()));
						return shapePlanNodeTuple;
					})
					.map(shapePlanNodeTuple -> () -> {

						PlanNode planNode = shapePlanNodeTuple.getPlanNode();
						ValidationExecutionLogger validationExecutionLogger = null;
						if (GlobalValidationExecutionLogging.loggingEnabled) {
							validationExecutionLogger = new ValidationExecutionLogger();
							planNode.receiveLogger(validationExecutionLogger);
						}

						// Important to start measuring time before we call .iterator() since the initialisation of the
						// iterator will already do a lot of work if there is for instance a Sort in the pipeline
						// because Sort (among others) will consume its parent iterator and sort the results on
						// initialization!
						long before = 0;
						if (sail.isPerformanceLogging()) {
							before = System.currentTimeMillis();
						}

						try (CloseableIteration<? extends ValidationTuple, SailException> iterator = planNode
								.iterator()) {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								logger.info("Start execution of plan:\n{}\n", shapePlanNodeTuple.getShape().toString());
							}

							ValidationResultIterator validationResults;
							try {
								validationResults = new ValidationResultIterator(iterator,
										sail.getEffectiveValidationResultsLimitPerConstraint());
							} finally {
								if (validationExecutionLogger != null) {
									validationExecutionLogger.flush();
								}
							}

							if (sail.isPerformanceLogging()) {
								long after = System.currentTimeMillis();
								logger.info("Execution of plan took {} ms for:\n{}\n", (after - before),
										shapePlanNodeTuple.getShape().toString());
							}

							if (GlobalValidationExecutionLogging.loggingEnabled) {
								logger.info("Finished execution of plan:\n{}\n",
										shapePlanNodeTuple.getShape().toString());

							}

							if (sail.isLogValidationViolations()) {
								if (!validationResults.conforms()) {
									List<ValidationTuple> tuples = validationResults.getTuples();

									logger.info(
											"SHACL not valid. The following experimental debug results were produced:  \n\t\t{}\n\n{}\n",
											tuples.stream()
													.map(ValidationTuple::toString)
													.collect(Collectors.joining("\n\t\t")),
											shapePlanNodeTuple.getShape().toString()

									);
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
		return transactionSettings.isParallelValidation();
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
		} finally {
			try {
				shapesRepoConnection.close();

			} finally {
				try {
					previousStateConnection.close();

				} finally {
					try {
						serializableConnection.close();

					} finally {
						try {
							previousStateSerializableConnection.close();

						} finally {
							try {
								super.close();

							} finally {
								try {
									sail.closeConnection(this);
								} finally {
									assert writeLock == null;
									assert readLock == null;

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
		flush();

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
				// only allow one transaction to modify the shapes at a time
				if (isShapeRefreshNeeded) {
					if (!(writeLock != null && writeLock.isActive())) {
						writeLock = sail.acquireExclusiveWriteLock(writeLock);
					}
				} else {
					if (!(readLock != null && readLock.isActive())) {
						readLock = sail.acquireReadLock();
					}
				}
			}

			List<Shape> shapesBeforeRefresh = sail.getCurrentShapes();
			List<Shape> shapesAfterRefresh;

			if (isShapeRefreshNeeded) {
				isShapeRefreshNeeded = false;
				shapesModifiedInCurrentTransaction = true;
				shapesAfterRefresh = sail.getShapes(shapesRepoConnection);
			} else {
				shapesAfterRefresh = shapesBeforeRefresh;
			}

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
				assert shapesBeforeRefresh != shapesAfterRefresh;

				HashSet<Shape> shapesBeforeRefreshSet = new HashSet<>(shapesBeforeRefresh);

				shapesAfterRefresh = shapesAfterRefresh.stream()
						.filter(shape -> !shapesBeforeRefreshSet.contains(shape))
						.collect(Collectors.toList());

			}

			prepareValidation();

			ValidationReport invalidTuples = null;
			if (useSerializableValidation) {
				synchronized (sail) {

					if (!sail.usesSingleConnection()) {
						invalidTuples = serializableValidation(shapesAfterRefresh);
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

				invalidTuples = validate(shapesAfterRefresh,
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

			preparedHasRun = true;

			shapesRepoConnection.prepare();
			previousStateConnection.prepare();
			super.prepare();

		}

	}

	private boolean isBulkValidation() {
		return transactionSettings.getValidationApproach() == ShaclSail.TransactionSettings.ValidationApproach.Bulk;
	}

	private ValidationReport serializableValidation(List<Shape> shapesAfterRefresh) {
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

					return performValidation(shapesAfterRefresh,
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

		prepareValidation();
		ValidationReport validate = validate(sail.getCurrentShapes(), true);

		return new ShaclSailValidationException(validate).getValidationReport();
	}

	Settings getTransactionSettings() {
		return transactionSettings;
	}

	public static class Settings {

		private ShaclSail.TransactionSettings.ValidationApproach validationApproach;
		private Boolean cacheSelectedNodes;
		private Boolean parallelValidation;
		private IsolationLevel isolationLevel;

		public Settings() {
		}

		public Settings(boolean cacheSelectNodes, boolean validationEnabled, boolean parallelValidation,
				IsolationLevel isolationLevel) {
			this.cacheSelectedNodes = cacheSelectNodes;
			if (!validationEnabled) {
				validationApproach = ShaclSail.TransactionSettings.ValidationApproach.Disabled;
			} else {
				this.validationApproach = ShaclSail.TransactionSettings.ValidationApproach.Auto;
			}
			this.parallelValidation = parallelValidation;
			this.isolationLevel = isolationLevel;
		}

		public ShaclSail.TransactionSettings.ValidationApproach getValidationApproach() {
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

		void applyTransactionSettings(Settings transactionSettingsLocal) {
			// apply restrictions first
			if (transactionSettingsLocal.validationApproach == ShaclSail.TransactionSettings.ValidationApproach.Bulk) {
				validationApproach = ShaclSail.TransactionSettings.ValidationApproach.Bulk;
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

			if (transactionSettingsLocal.validationApproach != null) {
				validationApproach = transactionSettingsLocal.validationApproach;
			}

			assert transactionSettingsLocal.isolationLevel == null;

			if (isolationLevel == IsolationLevels.SERIALIZABLE) {
				if (parallelValidation) {
					logger.warn("Parallel validation is not compatible with SERIALIZABLE isolation level!");
				}

				parallelValidation = false;
			}

		}
	}

	static class ShapePlanNodeTuple {
		private final Shape shape;
		private PlanNode planNode;

		public ShapePlanNodeTuple(Shape shape, PlanNode planNode) {
			this.shape = shape;
			this.planNode = planNode;
		}

		public Shape getShape() {
			return shape;
		}

		public PlanNode getPlanNode() {
			return planNode;
		}

		public void setPlanNode(PlanNode planNode) {
			this.planNode = planNode;
		}

		public boolean hasPlanNode() {
			return !(planNode instanceof EmptyNode);
		}
	}

}
