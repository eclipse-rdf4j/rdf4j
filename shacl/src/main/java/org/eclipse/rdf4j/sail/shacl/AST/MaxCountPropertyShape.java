/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * @author Heshan Jayasinghe
 */
public class MaxCountPropertyShape extends PathPropertyShape {

	Integer maxCount;

	public MaxCountPropertyShape(Resource next, SailRepositoryConnection connection) {
		super(next, connection);
		ValueFactory vf = connection.getValueFactory();
		//   maxCount = Integer.parseInt(connection.getStatements(next, vf.createIRI(SH.BASE_URI, "maxCount"), null, true).next().getObject().stringValue());

	}

	@Override
	public String toString() {
		return "MaxCountPropertyShape{" + "maxCount=" + maxCount + '}';
	}
}
