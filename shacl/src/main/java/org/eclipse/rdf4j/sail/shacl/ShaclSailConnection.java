/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.AST.NodeShape;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailConnection.class);

	private List<NodeShape> nodeShapes;

	private final NotifyingSailConnection previousStateConnection;
	private final NotifyingSailConnection serializableConnection;
	private final NotifyingSailConnection previousStateSerializableConnection;

	MemoryStore addedStatements;
	MemoryStore removedStatements;

	private HashSet<Statement> addedStatementsSet = new HashSet<>();
	private HashSet<Statement> removedStatementsSet = new HashSet<>();

	private boolean isShapeRefreshNeeded = false;
	private boolean shapesModifiedInCurrentTransaction = false;

	public final ShaclSail sail;

	private Stats stats;

	RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	private boolean preparedHasRun = false;

	private SailRepositoryConnection shapesRepoConnection;

	// write lock
	private long writeLockStamp;

	// used to determine if we are currently registered as a connection listener (getting added/removed notifications)
	private boolean connectionListenerActive = false;

	private IsolationLevel currentIsolationLevel = null;

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection,
			NotifyingSailConnection previousStateConnection, NotifyingSailConnection serializableConnection,
			NotifyingSailConnection previousStateSerializableConnection,
			SailRepositoryConnection shapesRepoConnection) {
		super(connection);
		this.previousStateConnection = previousStateConnection;
		this.serializableConnection = serializableConnection;
		this.previousStateSerializableConnection = previousStateSerializableConnection;
		this.shapesRepoConnection = shapesRepoConnection;
		this.sail = sail;

		setupConnectionListener();
	}

	@Override
	public void begin() throws SailException {
		begin(sail.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		currentIsolationLevel = level;
		assert addedStatements == null;
		assert removedStatements == null;

		stats = new Stats();

		// start two transactions, synchronize on underlying sail so that we get two transactions immediately
		// successively
		synchronized (sail) {
			super.begin(level);
			hasStatement(null, null, null, false); // actually force a transaction to start
			shapesRepoConnection.begin(level);
			previousStateConnection.begin(level);
			previousStateConnection.hasStatement(null, null, null, false); // actually force a transaction to start

		}

		stats.setBaseSailEmpty(isEmpty());
		if (stats.isBaseSailEmpty()) {
			removeConnectionListener(this);
			connectionListenerActive = false;
		} else {
			setupConnectionListener();
		}

	}

	private void setupConnectionListener() {
		if (!connectionListenerActive && sail.isValidationEnabled()) {
			addConnectionListener(this);

		}
	}

	private MemoryStore getNewMemorySail() {
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

		if (sail.holdsWriteLock(writeLockStamp)) {
			writeLockStamp = sail.releaseExclusiveWriteLock(writeLockStamp);
		}

		if (sail.isPerformanceLogging()) {
			logger.info("commit() excluding validation and cleanup took {} ms", System.currentTimeMillis() - before);
		}
		cleanup();
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLockStamp = sail.acquireExclusiveWriteLock(writeLockStamp);
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
			writeLockStamp = sail.acquireExclusiveWriteLock(writeLockStamp);
			shapesRepoConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLockStamp = sail.acquireExclusiveWriteLock(writeLockStamp);
			shapesRepoConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			writeLockStamp = sail.acquireExclusiveWriteLock(writeLockStamp);
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
		if (sail.holdsWriteLock(writeLockStamp)) {
			writeLockStamp = sail.releaseExclusiveWriteLock(writeLockStamp);
		}
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

		assert writeLockStamp == 0;
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

	private List<Tuple> validate(List<NodeShape> nodeShapes, boolean validateEntireBaseSail) {

		try {
			if (!sail.isValidationEnabled()) {
				return Collections.emptyList();
			}

			try (ConnectionsGroup connectionsGroup = getConnectionsGroup()) {
				return performValidation(nodeShapes, validateEntireBaseSail, connectionsGroup);
			}
		} finally {
			rdfsSubClassOfReasoner = null;

		}
	}

	private void prepareValidation() {

		if (!sail.isValidationEnabled()) {
			return;
		}

		if (sail.isRdfsSubClassReasoning()) {
			rdfsSubClassOfReasoner = RdfsSubClassOfReasoner.createReasoner(this);
		}

		fillAddedAndRemovedStatementRepositories();

	}

	ConnectionsGroup getConnectionsGroup() {

		return new ConnectionsGroup(sail, new VerySimpleRdfsBackwardsChainingConnection(this, rdfsSubClassOfReasoner),
				previousStateConnection, addedStatements, removedStatements, stats,
				this::getRdfsSubClassOfReasoner);
	}

	private static List<Tuple> performValidation(List<NodeShape> nodeShapes, boolean validateEntireBaseSail,
			ConnectionsGroup connectionsGroup) {
		long beforeValidation = 0;

		ShaclSail sail = connectionsGroup.getSail();

		if (sail.isPerformanceLogging()) {
			beforeValidation = System.currentTimeMillis();
		}

		try {
			Stream<PlanNode> planNodeStream = nodeShapes
					.stream()
					.flatMap(nodeShape -> nodeShape
							.generatePlans(connectionsGroup, nodeShape, sail.isLogValidationPlans(),
									validateEntireBaseSail));
			if (sail.isParallelValidation()) {
				planNodeStream = planNodeStream.parallel();
			}

			return planNodeStream.filter(Objects::nonNull).flatMap(planNode -> {
				ValidationExecutionLogger validationExecutionLogger = new ValidationExecutionLogger();
				planNode.receiveLogger(validationExecutionLogger);

				try (Stream<Tuple> stream = Iterations.stream(planNode.iterator())) {
					if (GlobalValidationExecutionLogging.loggingEnabled) {
						PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
						logger.info("Start execution of plan " + propertyShape.getNodeShape().toString() + " : "
								+ propertyShape.getId());
					}

					long before = 0;
					if (sail.isPerformanceLogging()) {
						before = System.currentTimeMillis();
					}

					List<Tuple> collect = stream.collect(Collectors.toList());
					validationExecutionLogger.flush();

					if (sail.isPerformanceLogging()) {
						long after = System.currentTimeMillis();
						PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
						logger.info("Execution of plan took {} ms for {} : {}", (after - before),
								propertyShape.getNodeShape().toString(), propertyShape.toString());
					}

					if (GlobalValidationExecutionLogging.loggingEnabled) {
						PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
						logger.info("Finished execution of plan {} : {}", propertyShape.getNodeShape().toString(),
								propertyShape.getId());
					}

					boolean valid = collect.size() == 0;

					if (!valid && sail.isLogValidationViolations()) {
						PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();

						logger.info(
								"SHACL not valid. The following experimental debug results were produced: \n\tNodeShape: {}\n\tPropertyShape: {} \n\t\t{}",
								propertyShape.getNodeShape().getId(), propertyShape.getId(),
								collect.stream()
										.map(a -> a.toString() + " -cause-> " + a.getCause())
										.collect(Collectors.joining("\n\t\t")));
					}

					return collect.stream();
				}
			}).collect(Collectors.toList());
		} finally {
			if (sail.isPerformanceLogging()) {
				logger.info("Actual validation and generating plans took {} ms",
						System.currentTimeMillis() - beforeValidation);
			}
		}
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
				addedStatements = (MemoryStore) sail.getBaseSail();
				removedStatements = getNewMemorySail();
			} else {
				addedStatements = getNewMemorySail();
				removedStatements = getNewMemorySail();

				try (Stream<? extends Statement> stream = Iterations.stream(getStatements(null, null, null, false))) {
					try (NotifyingSailConnection connection = addedStatements.getConnection()) {
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
						MemoryStore repository;
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
	}

	@Override
	public void prepare() throws SailException {
		flush();

		long readStamp = 0;

		try {
			long before = 0;
			if (sail.isPerformanceLogging()) {
				before = System.currentTimeMillis();
			}

			boolean useSerializableValidation = sail.isSerializableValidation()
					&& currentIsolationLevel == IsolationLevels.SNAPSHOT;

			if (useSerializableValidation) {
				if (!sail.holdsWriteLock(writeLockStamp)) {
					writeLockStamp = sail.acquireExclusiveWriteLock(writeLockStamp);
				}
			} else {
				if (!sail.holdsWriteLock(writeLockStamp)) {
					readStamp = sail.acquireReadlock();
				}
			}

			loadCachedNodeShapes();
			List<NodeShape> nodeShapesBeforeRefresh = this.nodeShapes;

			refreshShapes();

			List<NodeShape> nodeShapesAfterRefresh = this.nodeShapes;

			if (addedStatementsSet.isEmpty() && removedStatementsSet.isEmpty() && !shapesModifiedInCurrentTransaction) {
				boolean currentBaseSailEmpty = isEmpty();
				if (!(stats.isBaseSailEmpty() && !currentBaseSailEmpty)) {
					logger.debug("Nothing has changed, nothing to validate.");
					return;
				}
			}

			if (shapesModifiedInCurrentTransaction && addedStatementsSet.isEmpty() && removedStatementsSet.isEmpty()) {
				// we can optimize which shapes to revalidate since no data has changed.
				assert nodeShapesBeforeRefresh != nodeShapesAfterRefresh;

				HashSet<NodeShape> nodeShapesBeforeRefreshSet = new HashSet<>(nodeShapesBeforeRefresh);

				nodeShapesAfterRefresh = nodeShapesAfterRefresh.stream()
						.filter(nodeShape -> !nodeShapesBeforeRefreshSet.contains(nodeShape))
						.collect(Collectors.toList());

			}

			prepareValidation();

			List<Tuple> invalidTuples = null;
			if (useSerializableValidation) {
				synchronized (sail) {

					if (!sail.usesSingleConnection()) {
						invalidTuples = serializableValidation(nodeShapesAfterRefresh);
					}

				}
			}

			if (invalidTuples == null) {
				if (writeLockStamp != 0) {
					readStamp = sail.convertToReadLock(writeLockStamp);
					writeLockStamp = 0;
				}
				invalidTuples = validate(nodeShapesAfterRefresh, shapesModifiedInCurrentTransaction);
			}

			boolean valid = invalidTuples.isEmpty();

			if (sail.isPerformanceLogging()) {
				logger.info("prepare() including validation excluding locking and super.prepare() took {} ms",
						System.currentTimeMillis() - before);
			}

			if (!valid) {
				throw new ShaclSailValidationException(invalidTuples);
			}
		} finally {
			preparedHasRun = true;

			if (readStamp != 0 && !sail.holdsWriteLock(writeLockStamp)) {
				readStamp = sail.releaseReadlock(readStamp);
			}
			previousStateConnection.prepare();
			super.prepare();
		}

	}

	private List<Tuple> serializableValidation(List<NodeShape> nodeShapesAfterRefresh) {
		List<Tuple> invalidTuples;
		try {
			try {
				try (ConnectionsGroup connectionsGroup = new ConnectionsGroup(sail,
						new VerySimpleRdfsBackwardsChainingConnection(serializableConnection, rdfsSubClassOfReasoner),
						previousStateSerializableConnection, addedStatements, removedStatements, stats,
						() -> getRdfsSubClassOfReasoner())) {

					connectionsGroup.getBaseConnection().begin(IsolationLevels.SNAPSHOT);
					connectionsGroup.getBaseConnection().hasStatement(null, null, null, false); // actually force a
					// transaction to start

					connectionsGroup.getPreviousStateConnection().begin(IsolationLevels.SNAPSHOT);
					connectionsGroup.getPreviousStateConnection().hasStatement(null, null, null, false); // actually
																											// force a
					// transaction
					// to start

					stats.setBaseSailEmpty(ConnectionHelper.isEmpty(connectionsGroup.getBaseConnection()));

					try (NotifyingSailConnection connection = addedStatements.getConnection()) {
						SailConnection baseConnection = connectionsGroup.getBaseConnection();
						ConnectionHelper.transferStatements(connection, baseConnection::addStatement);
					}

					try (NotifyingSailConnection connection = removedStatements.getConnection()) {
						SailConnection baseConnection = connectionsGroup.getBaseConnection();
						ConnectionHelper.transferStatements(connection, baseConnection::removeStatements);

					}

					serializableConnection.flush();

					invalidTuples = performValidation(nodeShapesAfterRefresh,
							shapesModifiedInCurrentTransaction, connectionsGroup);

				} finally {
					serializableConnection.rollback();
				}
			} finally {
				previousStateSerializableConnection.rollback();
			}
		} finally {
			rdfsSubClassOfReasoner = null;

		}
		return invalidTuples;
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
		List<Tuple> validate = validate(this.nodeShapes, true);

		return new ShaclSailValidationException(validate).getValidationReport();
	}

}
