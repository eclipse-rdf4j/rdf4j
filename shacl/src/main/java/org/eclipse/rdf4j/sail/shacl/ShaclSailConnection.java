/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.sail.shacl.AST.Shape;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.List;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSailConnection extends NotifyingSailConnectionWrapper {

	public ShaclSail sail;


	 Repository addedStatements;
	 Repository removedStatements;

	ShaclSailConnection(ShaclSail shaclSail, NotifyingSailConnection connection) {
		super(connection);
		this.sail = shaclSail;

		addConnectionListener(new SailConnectionListener() {

								  @Override
								  public void statementAdded(Statement statement) {
									  try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
										  addedStatementsConnection.add(statement);
									  }
									  try (RepositoryConnection removedStatementsConnection = removedStatements.getConnection()) {
										  removedStatementsConnection.remove(statement);
									  }
								  }

								  @Override
								  public void statementRemoved(Statement statement) {
									  try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
										  addedStatementsConnection.remove(statement);
									  }
									  try (RepositoryConnection removedStatementsConnection = removedStatements.getConnection()) {
										  removedStatementsConnection.add(statement);
									  }
								  }
							  }

		);
	}

	@Override
	public void begin(IsolationLevel level)
			throws SailException
	{
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
			throws SailException
	{
		try {
			boolean valid = validate();
			if(!valid){
				rollback();
				throw new SailException("Failed SHACL validation");
			}else{
				super.commit();
			}
		}finally {
			cleanup();
		}
	}

	@Override
	public void rollback() throws SailException {
		cleanup();
		super.rollback();
	}

	private void cleanup() {
		if(addedStatements != null) {
			addedStatements.shutDown();
			addedStatements = null;
		}
		if(removedStatements != null) {
			removedStatements.shutDown();
			removedStatements = null;
		}
	}


	private boolean validate() {

		boolean allValid = true;

		for (Shape shape : sail.shapes) {
			List<PlanNode> planNodes = shape.generatePlans(this, shape);
			for (PlanNode planNode : planNodes) {
				boolean valid = planNode.validate();
				allValid = allValid && valid;
			}
		}

		return allValid;
	}

	@Override
	synchronized public void close() throws SailException {
		if(isActive()){
			rollback();
		}
		super.close();
	}
}
