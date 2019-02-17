/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;

import java.util.Collections;
import java.util.List;

/**
 * The AST (Abstract Syntax Tree) node that represents the sh:path on a property nodeShape.
 *
 * @author Heshan Jayasinghe
 */
public class PathPropertyShape extends PropertyShape {

	Path path;

	PathPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, nodeShape);

		// only simple path is supported. There are also no checks. Any use of paths that are not single predicates is undefined.
		path = new SimplePath(id, connection);

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {
		return shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection, path.getQuery("?a", "?c")));
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		return shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getAddedStatements(), path.getQuery("?a", "?c")));

	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		return shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getRemovedStatements(), path.getQuery("?a", "?c")));

	}

	@Override
	public List<Path> getPaths() {
		return Collections.singletonList(path);
	}


	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return super.requiresEvaluation(addedStatements, removedStatements) || path.requiresEvaluation(addedStatements, removedStatements);
	}


	public Path getPath() {
		return path;
	}
}

