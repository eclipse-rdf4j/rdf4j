/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;

/**
 * @author Heshan Jayasinghe
 */
public class PathPropertyShape extends PropertyShape {

	Path path;

	PathPropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, shape);

		path = new Path(id, connection);

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
		Select select = new Select(shaclSailConnection, path.getQuery());
		return select;
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, Shape shape) {
		return new Select(shaclSailConnection.addedStatements, path.getQuery());
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, Shape shape) {
		return new Select(shaclSailConnection.removedStatements, path.getQuery());
	}


	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {
		return super.requiresEvalutation(addedStatements, removedStatements) || path.requiresEvalutation(addedStatements, removedStatements);
	}
}

