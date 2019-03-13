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
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailConnection.class);

	private NotifyingSailConnection previousStateConnection;

	MemoryStore addedStatements;
	MemoryStore removedStatements;

	private ConcurrentLinkedQueue<SailConnection> connectionsToClose = new ConcurrentLinkedQueue<>();

	private HashSet<Statement> addedStatementsSet = new HashSet<>();
	private HashSet<Statement> removedStatementsSet = new HashSet<>();

	private boolean isShapeRefreshNeeded = false;

	public final ShaclSail sail;

	public Stats stats;

	RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	private boolean preparedHasRun = false;

	private SailRepositoryConnection shapesConnection;

	// used to cache Select plan nodes so that we don't query a store for the same data during the same validation step.
	private Map<Select, BufferedSplitter> selectNodeCache;

	// used to indicate if the transaction is in the validating phase
	boolean validating;

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection,
						NotifyingSailConnection previousStateConnection, SailRepositoryConnection shapesConnection) {
		super(connection);
		this.previousStateConnection = previousStateConnection;
		this.shapesConnection = shapesConnection;
		this.sail = sail;

		if (sail.isValidationEnabled()) {
			addConnectionListener(this);
		}
	}

	public NotifyingSailConnection getPreviousStateConnection() {
		return previousStateConnection;
	}

	public SailConnection getAddedStatements() {
		NotifyingSailConnection connection = addedStatements.getConnection();
		connectionsToClose.add(connection);
		return connection;
	}

	public SailConnection getRemovedStatements() {
		NotifyingSailConnection connection = removedStatements.getConnection();
		connectionsToClose.add(connection);
		return connection;
	}

	@Override
	public void begin() throws SailException {
		begin(sail.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {

		assert addedStatements == null;
		assert removedStatements == null;
		assert connectionsToClose.size() == 0;


		stats = new Stats();

		// start two transactions, synchronize on underlying sail so that we get two transactions immediatly successivley
		synchronized (sail) {
			super.begin(level);
			shapesConnection.begin(level);
			previousStateConnection.begin(level);
		}

		stats.baseSailEmpty = !hasStatement(null, null, null, true);

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
		previousStateConnection.commit();
		super.commit();
		shapesConnection.commit();
		cleanup();


	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.addStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj,
								Resource... contexts)
		throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException {
		if (contexts.length == 1 && contexts[0].equals(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		} else {
			super.removeStatements(subj, pred, obj, contexts);
		}
	}

	@Override
	public void rollback() throws SailException {
		synchronized (sail) {
			previousStateConnection.rollback();
			shapesConnection.rollback();
			super.rollback();
			cleanup();
			refreshShapes(shapesConnection);
		}
	}

	void cleanup() {
		logger.debug("Cleanup");
		connectionsToClose.forEach(SailConnection::close);
		connectionsToClose = new ConcurrentLinkedQueue<>();

		if (addedStatements != null) {
			addedStatements.shutDown();
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
		selectNodeCache = null;
	}

	private List<NodeShape> refreshShapes(SailRepositoryConnection shapesRepoConnection) {
		List<NodeShape> nodeShapes = sail.getNodeShapes();
		if (isShapeRefreshNeeded) {
			nodeShapes = sail.refreshShapes(shapesRepoConnection);
			isShapeRefreshNeeded = false;
		}

		return nodeShapes;
	}

	private List<Tuple> validate() {

		if (!sail.isValidationEnabled()) {
			return Collections.emptyList();
		}

		if(sail.isRdfsSubClassReasoning()) {
			rdfsSubClassOfReasoner = RdfsSubClassOfReasoner.createReasoner(this);
		}

		try {
			validating = true;

			fillAddedAndRemovedStatementRepositories();

			try {
				Stream<PlanNode> planNodeStream = sail
					.getNodeShapes()
					.stream()
					.flatMap(nodeShape -> nodeShape.generatePlans(this, nodeShape, sail.isLogValidationPlans()).stream());
				if (sail.isParallelValidation()) {
					planNodeStream = planNodeStream.parallel();
				}

				return planNodeStream
					.flatMap(planNode -> {
						try (Stream<Tuple> stream = Iterations.stream(planNode.iterator())) {
							if (LoggingNode.loggingEnabled) {
								PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
								logger.info("Start execution of plan " + propertyShape.getNodeShape().toString() + " : " + propertyShape.getId());
							}

							List<Tuple> collect = stream.collect(Collectors.toList());

							if (LoggingNode.loggingEnabled) {
								PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
								logger.info("Finished execution of plan {} : {}", propertyShape.getNodeShape().toString(), propertyShape.getId());
							}


							boolean valid = collect.size() == 0;

							if (!valid && sail.isLogValidationViolations()) {
								PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();

								logger.info(
									"SHACL not valid. The following experimental debug results were produced: \n\tNodeShape: {}\n\tPropertyShape: {} \n\t\t{}",
									propertyShape.getNodeShape().getId(),
									propertyShape.getId(),
									collect
										.stream()
										.map(a -> a.toString() + " -cause-> " + a.getCause())
										.collect(Collectors.joining("\n\t\t")));
							}

							return collect.stream();
						}
					})
					.collect(Collectors.toList());
			} finally {
				connectionsToClose.forEach(SailConnection::close);
				connectionsToClose = new ConcurrentLinkedQueue<>();
			}
		} finally {
			validating = false;
			rdfsSubClassOfReasoner = null;
		}
	}

	void fillAddedAndRemovedStatementRepositories() {

		connectionsToClose.forEach(SailConnection::close);
		connectionsToClose = new ConcurrentLinkedQueue<>();

		if (addedStatements != null) {
			addedStatements.shutDown();
			addedStatements = null;
		}
		if (removedStatements != null) {
			removedStatements.shutDown();
			removedStatements = null;
		}

		addedStatements = getNewMemorySail();
		removedStatements = getNewMemorySail();

		addedStatementsSet.forEach(stats::added);
		removedStatementsSet.forEach(stats::removed);

		try (SailConnection connection = addedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			addedStatementsSet
				.stream()
				.filter(statement -> !removedStatementsSet.contains(statement))
				.flatMap(statement -> rdfsSubClassOfReasoner == null ? Stream.of(statement) : rdfsSubClassOfReasoner.forwardChain(statement))
				.forEach(statement -> connection.addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext()));
			connection.commit();
		}

		try (SailConnection connection = removedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			removedStatementsSet
				.stream()
				.filter(statement -> !addedStatementsSet.contains(statement))
				.flatMap(statement -> rdfsSubClassOfReasoner == null ? Stream.of(statement) : rdfsSubClassOfReasoner.forwardChain(statement))
				.forEach(statement -> connection.addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext()));
			connection.commit();
		}

		selectNodeCache = new HashMap<>();

	}

	@Override
	synchronized public void close() throws SailException {
		if (isActive()) {
			rollback();
		}
		shapesConnection.close();
		previousStateConnection.close();
		super.close();
		connectionsToClose.forEach(SailConnection::close);
		connectionsToClose = new ConcurrentLinkedQueue<>();
	}

	@Override
	public void prepare() throws SailException {
		try {
			preparedHasRun = true;

			List<NodeShape> nodeShapes = refreshShapes(shapesConnection);

			// we don't support revalidation of all data when changing the shacl shapes,
			// so no need to check if the shapes have changed
			if (addedStatementsSet.isEmpty() && removedStatementsSet.isEmpty()) {
				logger.debug("Nothing has changed, nothing to validate.");
				return;
			}

			if (!sail.isIgnoreNoShapesLoadedException()
				&& ((!addedStatementsSet.isEmpty() || !removedStatementsSet.isEmpty())
				&& nodeShapes.isEmpty())) {
				throw new NoShapesLoadedException();
			}

			List<Tuple> invalidTuples = validate();
			boolean valid = invalidTuples.isEmpty();

			if (!valid) {
				throw new ShaclSailValidationException(invalidTuples);
			}
		}finally {
			super.prepare();
			previousStateConnection.prepare();
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


	synchronized public PlanNode getCachedNodeFor(Select select) {

		if (!sail.isCacheSelectNodes()) {
			return select;
		}

		BufferedSplitter bufferedSplitter = selectNodeCache.computeIfAbsent(select, BufferedSplitter::new);

		return bufferedSplitter.getPlanNode();
	}

	public RdfsSubClassOfReasoner getRdfsSubClassOfReasoner() {
		return rdfsSubClassOfReasoner;
	}

	public class Stats {

		boolean baseSailEmpty;
		boolean hasAdded;
		boolean hasRemoved;

		public void added(Statement statement) {
			hasAdded = true;
		}

		public void removed(Statement statement) {
			hasRemoved = true;

		}

		public boolean hasAdded() {
			return hasAdded;
		}

		public boolean hasRemoved() {
			return hasRemoved;
		}

		public boolean isBaseSailEmpty() {
			return baseSailEmpty;
		}
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		if (rdfsSubClassOfReasoner != null &&  includeInferred && validating && obj instanceof Resource && RDF.TYPE.equals(pred)) {
			Set<Resource> inferredTypes = rdfsSubClassOfReasoner.backwardsChain((Resource) obj);
			if (!inferredTypes.isEmpty()) {

				CloseableIteration<Statement, SailException>[] statementsMatchingInferredTypes = inferredTypes
					.stream()
					.map(r -> super.getStatements(subj, pred, r, false, contexts))
					.toArray(CloseableIteration[]::new);


				return new CloseableIteration<Statement, SailException>() {

					UnionIteration<Statement, SailException> unionIteration = new UnionIteration<>(statementsMatchingInferredTypes);

					Statement next = null;

					HashSet<Statement> dedupe = new HashSet<>();

					private void calculateNext() {
						if (next != null) {
							return;
						}

						while (next == null && unionIteration.hasNext()) {
							Statement temp = unionIteration.next();
							temp = SimpleValueFactory.getInstance().createStatement(temp.getSubject(), temp.getPredicate(), obj, temp.getContext());

							if(!dedupe.isEmpty()){
								boolean contains = dedupe.contains(temp);
								if(!contains){
									next = temp;
									dedupe.add(next);
								}
							}else{
								next = temp;
								dedupe.add(next);
							}

						}
					}


					@Override
					public boolean hasNext() throws SailException {
						calculateNext();
						return next != null;
					}

					@Override
					public Statement next() throws SailException {
						calculateNext();
						Statement temp = next;
						next = null;
						return temp;
					}

					@Override
					public void remove() throws SailException {
						unionIteration.remove();
					}

					@Override
					public void close() throws SailException {
						unionIteration.close();
					}
				};

			}
		}

		return super.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		boolean hasStatement = super.hasStatement(subj, pred, obj, includeInferred, contexts);

		if (rdfsSubClassOfReasoner != null && includeInferred && validating && obj instanceof Resource && RDF.TYPE.equals(pred)) {
			return hasStatement | rdfsSubClassOfReasoner
				.backwardsChain((Resource) obj)
				.stream()
				.map(type -> super.hasStatement(subj, pred, type, false, contexts))
				.reduce((a, b) -> a || b).orElse(false);
		}
		return hasStatement;
	}

}
