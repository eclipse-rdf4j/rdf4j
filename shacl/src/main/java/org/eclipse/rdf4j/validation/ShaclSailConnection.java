/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.AST.Shape;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.plan.PlanNode;
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

	{
		addedStatements = new SailRepository(new MemoryStore());
		addedStatements.initialize();
		removedStatements = new SailRepository(new MemoryStore());
		removedStatements.initialize();
	}


	ShaclSailConnection(ShaclSail shaclSail, NotifyingSailConnection connection) {
		super(connection);
		this.sail = shaclSail;

		addConnectionListener(new SailConnectionListener() {

								  @Override
								  public void statementAdded(Statement statement) {
									  try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
										  addedStatementsConnection.add(statement);
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
		super.begin(level);
	}

	@Override
	public void commit()
			throws SailException
	{
		super.commit();

		for (Shape shape : sail.shapes) {
			List<PlanNode> planNodes = shape.generatePlans(this, shape);
			for (PlanNode planNode : planNodes) {
				boolean valid = planNode.validate();
				if (!valid) {
					throw new SailException("invalid for shacl");
				}
			}
		}
	}

	protected Model createModel() {
		return new TreeModel();
	}

	;

}
