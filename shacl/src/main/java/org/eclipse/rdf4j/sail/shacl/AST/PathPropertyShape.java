/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Select;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.validation.ShaclSailConnection;

/**
 * @author Heshan Jayasinghe
 */
public class PathPropertyShape extends PropertyShape implements PlanGenerator {

	public Path path;

	public PathPropertyShape(Resource id, SailRepositoryConnection connection) {
		super(id, connection);
		this.id = id;
		this.connection = connection;

		if (connection.hasStatement(id, SHACL.PATH, null, true)) {
			path = new Path(id, connection);
		}

	}

	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
		Select select = new Select(shaclSailConnection, null, (IRI)path.path, null);
		return select;
	}
}

