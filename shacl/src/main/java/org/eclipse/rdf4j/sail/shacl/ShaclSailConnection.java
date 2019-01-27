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
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.AST.NodeShape;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailConnection.class);

	private NotifyingSailConnection previousStateConnection;

	private Repository addedStatements;

	private Repository removedStatements;

	private boolean isShapeRefreshNeeded = false;

	public final ShaclSail sail;

	public Stats stats;

	private HashSet<Statement> addedStatementsSet = new HashSet<>();

	private HashSet<Statement> removedStatementsSet = new HashSet<>();

	private SailRepositoryConnection shapesConnection;

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection,
			NotifyingSailConnection previousStateConnection, SailRepositoryConnection shapesConnection)
	{
		super(connection);
		this.previousStateConnection = previousStateConnection;
		this.shapesConnection = shapesConnection;
		this.sail = sail;

		if (sail.config.validationEnabled) {

			addConnectionListener(new SailConnectionListener() {

				@Override
				public void statementAdded(Statement statement) {
					boolean add = addedStatementsSet.add(statement);
					if (!add) {
						removedStatementsSet.remove(statement);
					}

				}

				@Override
				public void statementRemoved(Statement statement) {
					boolean add = removedStatementsSet.add(statement);
					if (!add) {
						addedStatementsSet.remove(statement);
					}
				}
			}

			);
		}
	}

	public NotifyingSailConnection getPreviousStateConnection() {
		return previousStateConnection;
	}

	public Repository getAddedStatements() {
		return addedStatements;
	}

	public Repository getRemovedStatements() {
		return removedStatements;
	}

	@Override
	public void begin() throws SailException {
		begin(sail.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {

		assert addedStatements == null;
		assert removedStatements == null;

		stats = new Stats();

		// start two transactions, synchronize on underlying sail so that we get two transactions immediatly successivley
		synchronized (sail) {
			super.begin(level);
			shapesConnection.begin(IsolationLevels.SERIALIZABLE);
			previousStateConnection.begin(IsolationLevels.SNAPSHOT);
		}

	}

	private SailRepository getNewMemorySail() {
		MemoryStore sail = new MemoryStore();
		sail.setDefaultIsolationLevel(IsolationLevels.NONE);
		SailRepository repository = new SailRepository(sail);
		repository.initialize();
		return repository;
	}

	@Override
	public void commit() throws SailException {
		synchronized (sail) {

			refreshShapes(shapesConnection);

			if (!sail.isIgnoreNoShapesLoadedException()
					&& ((!addedStatementsSet.isEmpty() || !removedStatementsSet.isEmpty())
							&& sail.getNodeShapes().isEmpty()))
			{
				throw new NoShapesLoadedException();
			}

			try {
				List<Tuple> invalidTuples = validate();
				boolean valid = invalidTuples.isEmpty();
				previousStateConnection.commit();

				if (!valid) {
					rollback();
					refreshShapes(shapesConnection);
					throw new ShaclSailValidationException(invalidTuples);
				}
				else {
					shapesConnection.commit();
					super.commit();
				}
			}
			finally {
				cleanup();
			}
		}
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		if (contexts.length == 1 && contexts[0].equals(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		}
		else {
			super.addStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, IRI pred, Value obj,
			Resource... contexts)
		throws SailException
	{
		if (contexts.length == 1 && contexts[0].equals(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		}
		else {
			super.removeStatement(modify, subj, pred, obj, contexts);
		}
	}

	@Override
	public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 1 && RDF4J.SHACL_SHAPE_GRAPH.equals(contexts[0])) {
			shapesConnection.add(subj, pred, obj);
			isShapeRefreshNeeded = true;
		}
		else {
			super.addStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		if (contexts.length == 1 && contexts[0].equals(RDF4J.SHACL_SHAPE_GRAPH)) {
			shapesConnection.remove(subj, pred, obj);
			isShapeRefreshNeeded = true;
		}
		else {
			super.removeStatements(subj, pred, obj, contexts);
		}
	}

	@Override
	public void rollback() throws SailException {
		synchronized (sail) {
			previousStateConnection.commit();
			cleanup();
			shapesConnection.rollback();
			super.rollback();
		}
	}

	private void cleanup() {
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
		isShapeRefreshNeeded = false;
	}

	private void refreshShapes(SailRepositoryConnection shapesRepoConnection) {
		if (isShapeRefreshNeeded) {
			this.sail.refreshShapes(shapesRepoConnection);
			isShapeRefreshNeeded = false;
		}
	}

	private List<Tuple> validate() {

		if (!sail.config.validationEnabled) {
			return Collections.emptyList();
		}

		fillAddedAndRemovedStatementRepositories();

		List<Tuple> ret = new ArrayList<>();

		final List<NodeShape> nodeShapes = NodeShape.Factory.getShapes(shapesConnection);
		for (NodeShape nodeShape : nodeShapes) {
			List<PlanNode> planNodes = nodeShape.generatePlans(this, nodeShape, sail.config.logValidationPlans);
			for (PlanNode planNode : planNodes) {
				try (Stream<Tuple> stream = Iterations.stream(planNode.iterator())) {
					if(LoggingNode.loggingEnabled){
						PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
						logger.info("Start execution of plan "+nodeShape.toString()+" : "+propertyShape.getId());
					}
					List<Tuple> collect = stream.collect(Collectors.toList());

					if(LoggingNode.loggingEnabled){
						PropertyShape propertyShape = ((EnrichWithShape) planNode).getPropertyShape();
						logger.info("Finished execution of plan "+nodeShape.toString()+" : "+propertyShape.getId());
					}
					ret.addAll(collect);



					boolean valid = collect.size() == 0;
					if (!valid && sail.config.logValidationViolations) {
						logger.info(
							"SHACL not valid. The following experimental debug results were produced: \n\tNodeShape: {} \n\t\t{}",
							nodeShape.toString(),
							collect
								.stream()
								.map(a -> a.toString() + " -cause-> " + a.getCause())
								.collect(Collectors.joining("\n\t\t")));
					}
				}
			}
		}

		return ret;
	}

	void fillAddedAndRemovedStatementRepositories() {

		addedStatements = getNewMemorySail();
		removedStatements = getNewMemorySail();

		addedStatementsSet.forEach(stats::added);
		removedStatementsSet.forEach(stats::removed);

		try (RepositoryConnection connection = addedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			addedStatementsSet.stream().filter(
					statement -> !removedStatementsSet.contains(statement)).forEach(connection::add);
			connection.commit();
		}

		try (RepositoryConnection connection = removedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			removedStatementsSet.stream().filter(
					statement -> !addedStatementsSet.contains(statement)).forEach(connection::add);
			connection.commit();
		}
	}

	@Override
	synchronized public void close() throws SailException {
		if (isActive()) {
			rollback();
		}
		shapesConnection.close();
		previousStateConnection.close();
		super.close();
	}

	public class Stats {

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
	}
}
