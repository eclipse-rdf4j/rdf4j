/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper {

	public ShaclSail sail;


	public Repository addedStatements;
	public Repository removedStatements;

	 private HashSet<Statement> addedStatementsSet = new HashSet<>();
	 private HashSet<Statement> removedStatementsSet = new HashSet<>();

	ShaclSailConnection(ShaclSail sail, NotifyingSailConnection connection) {
		super(connection);
		this.sail = sail;

		if (sail.config.validationEnabled) {

			addConnectionListener(new SailConnectionListener() {

									  @Override
									  public void statementAdded(Statement statement) {

									  	addedStatementsSet.add(statement);
									  	removedStatementsSet.remove(statement);

//										  try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
//											  addedStatementsConnection.begin(IsolationLevels.NONE);
//											  addedStatementsConnection.add(statement);
//											  addedStatementsConnection.commit();
//										  }
//										  try (RepositoryConnection removedStatementsConnection = removedStatements.getConnection()) {
//											  removedStatementsConnection.begin(IsolationLevels.NONE);
//											  removedStatementsConnection.remove(statement);
//											  removedStatementsConnection.commit();
//										  }
									  }

									  @Override
									  public void statementRemoved(Statement statement) {
									  	removedStatementsSet.add(statement);
									  	addedStatementsSet.remove(statement);
//										  try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
//											  addedStatementsConnection.begin(IsolationLevels.NONE);
//											  addedStatementsConnection.remove(statement);
//											  addedStatementsConnection.commit();
//										  }
//										  try (RepositoryConnection removedStatementsConnection = removedStatements.getConnection()) {
//											  removedStatementsConnection.begin(IsolationLevels.NONE);
//											  removedStatementsConnection.add(statement);
//											  removedStatementsConnection.commit();
//										  }
									  }
								  }

			);
		}
	}

	@Override
	public void begin(IsolationLevel level)
		throws SailException {

		assert addedStatements == null;
		assert removedStatements == null;

		addedStatements = new SailRepository(new MemoryStore());
		addedStatements.initialize();
		removedStatements = new SailRepository(new MemoryStore());
		removedStatements.initialize();


		super.begin(level);
	}

	@Override
	public void commit()
		throws SailException {
		try {
			boolean valid = validate();
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

	@Override
	public void rollback() throws SailException {
		cleanup();
		super.rollback();
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
	}


	private boolean validate() {

		if (!sail.config.validationEnabled) {
			return true;
		}

		try (RepositoryConnection connection = addedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(addedStatementsSet);
			connection.commit();
		}

		try (RepositoryConnection connection = removedStatements.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(removedStatementsSet);
			connection.commit();
		}


		boolean allValid = true;

		for (Shape shape : sail.shapes) {
			List<PlanNode> planNodes = shape.generatePlans(this, shape);
			for (PlanNode planNode : planNodes) {



				try (Stream<Tuple> stream = Iterations.stream(planNode.iterator())) {
					List<Tuple> collect = stream.collect(Collectors.toList());



					boolean valid = collect.size() == 0;
					if(!valid){
						System.out.println("-----------------------------------------");
						collect.forEach(System.out::println);
						System.out.println("-----------------------------------------");

					}
					allValid = allValid && valid;
				}
			}
		}

		return allValid;
	}

	@Override
	synchronized public void close() throws SailException {
		if (isActive()) {
			rollback();
		}
		super.close();
	}
}
