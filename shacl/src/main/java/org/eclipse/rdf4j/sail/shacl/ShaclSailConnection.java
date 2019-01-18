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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.AST.Shape;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private NotifyingSailConnection previousStateConnection;
	private Repository addedStatements;
	private Repository removedStatements;

	public final ShaclSail sail;

	public Stats stats;

	private HashSet<Statement> addedStatementsSet = new HashSet<>();
	private HashSet<Statement> removedStatementsSet = new HashSet<>();

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection, NotifyingSailConnection previousStateConnection) {
		super(connection);
		this.previousStateConnection = previousStateConnection;
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
	public void begin(IsolationLevel level)
		throws SailException {

		assert addedStatements == null;
		assert removedStatements == null;

		stats = new Stats();

		// start two transactions, synchronize on underlying sail so that we get two transactions immediatly successivley
		synchronized (sail){
			super.begin(level);
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
	public void commit()
		throws SailException {
		synchronized (sail) {
			try {
				boolean valid = validate();
				previousStateConnection.commit();

				if (!valid) {
					rollback();
					throw new SailException("Failed SHACL validation");
				} else {
					super.commit();
				}
			} finally {
				cleanup();
			}
		}
	}

	@Override
	public void rollback() throws SailException {
		synchronized (sail) {
			previousStateConnection.commit();
			cleanup();
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
	}


	private boolean validate() {

		if (!sail.config.validationEnabled) {
			return true;
		}

		fillAddedAndRemovedStatementRepositories();

		boolean allValid = true;

		for (Shape shape : sail.shapes) {
			List<PlanNode> planNodes = shape.generatePlans(this, shape);
			for (PlanNode planNode : planNodes) {
				try (Stream<Tuple> stream = Iterations.stream(planNode.iterator())) {
					List<Tuple> collect = stream.collect(Collectors.toList());

					boolean valid = collect.size() == 0;
					if (!valid) {
						logger.warn("SHACL not valid. The following experimental debug results were produced: \n\tShape: {} \n\t\t{}", shape.toString(), String.join("\n\t\t", collect.stream().map(a -> a.toString()+" -cause-> "+a.getCause()).collect(Collectors.toList())));
					}
					allValid = allValid && valid;
				}
			}
		}

		return allValid;
	}

	void fillAddedAndRemovedStatementRepositories() {

		addedStatements = getNewMemorySail();
		removedStatements = getNewMemorySail();


		addedStatementsSet.forEach(stats::added);
		removedStatementsSet.forEach(stats::removed);


		try (RepositoryConnection connection = addedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			addedStatementsSet.stream().filter(statement -> !removedStatementsSet.contains(statement)).forEach(connection::add);
			connection.commit();
		}

		try (RepositoryConnection connection = removedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			removedStatementsSet.stream().filter(statement -> !addedStatementsSet.contains(statement)).forEach(connection::add);
			connection.commit();
		}
	}

	@Override
	synchronized public void close() throws SailException {
		if (isActive()) {
			rollback();
		}
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

