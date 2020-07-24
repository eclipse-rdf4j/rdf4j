/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * The AST (Abstract Syntax Tree) node that represents a simple path for exactly one predicate. Currently there is no
 * support for complex paths.
 *
 * @author Heshan Jayasinghe
 * @author Håvard M. Ottestad
 */
abstract public class Path implements RequiresEvalutation, QueryGenerator, PlanGenerator {

	private Resource id;

	Path(Resource id) {
		this.id = id;

	}

	public Resource getId() {
		return id;
	}

	public abstract Stream<StatementPattern> getStatementsPatterns(Var start, Var end);
}
