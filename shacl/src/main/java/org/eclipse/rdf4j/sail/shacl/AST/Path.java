/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;

/**
 * The AST (Abstract Syntax Tree) node that represents a simple path for exactly one predicate. Currently there is no support for complex paths.
 *
 * @author Heshan Jayasinghe
 */
abstract public class Path implements RequiresEvalutation, QueryGenerator {

	private Resource id;


	Path(Resource id) {
		this.id = id;

	}


	public Resource getId() {
		return id;
	}
}
