/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * A QueryModelVisitor that collects the names of (non-constant) variables that are used in a query model.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class VarNameCollector extends AbstractQueryModelVisitor<RuntimeException> {

	public static Set<String> process(QueryModelNode node) {
		VarNameCollector collector = new VarNameCollector();
		node.visit(collector);
		return collector.getVarNames();
	}

	private final Set<String> varNames = new LinkedHashSet<>();

	public Set<String> getVarNames() {
		return varNames;
	}

	@Override
	public void meet(Var var) {
		if (!var.hasValue()) {
			varNames.add(var.getName());
		}
	}
}
