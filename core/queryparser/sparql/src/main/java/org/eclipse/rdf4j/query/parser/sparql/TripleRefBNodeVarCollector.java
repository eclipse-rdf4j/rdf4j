/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public class TripleRefBNodeVarCollector extends AbstractQueryModelVisitor<RuntimeException> {

	public static Set<Var> process(QueryModelNode node) {
		TripleRefBNodeVarCollector collector = new TripleRefBNodeVarCollector();
		node.visit(collector);
		return collector.getTripleRefs();
	}

	private final Set<Var> tripleRefs = new HashSet<>();

	public Set<Var> getTripleRefs() {
		return tripleRefs;
	}

	@Override
	public void meet(Filter node) {
		// Skip boolean constraints
		node.getArg().visit(this);
	}

	@Override
	public void meet(TripleRef node) {
		if (isValidBNode(node.getSubjectVar())) {
			tripleRefs.add(node.getSubjectVar());
		}
		if (isValidBNode(node.getObjectVar())) {
			tripleRefs.add(node.getObjectVar());
		}
	}

	@Override
	public void meet(ValueExprTripleRef node) {
		if (isValidBNode(node.getSubjectVar())) {
			tripleRefs.add(node.getSubjectVar());
		}
		if (isValidBNode(node.getObjectVar())) {
			tripleRefs.add(node.getObjectVar());
		}
	}

	@Override
	public void meetOther(QueryModelNode node) {
		if (node instanceof TripleRef) {
			this.meet((TripleRef) node);
		} else if (node instanceof ValueExprTripleRef) {
			this.meet((ValueExprTripleRef) node);
		} else {
			super.meetOther(node);
		}
	}

	private boolean isValidBNode(Var var) {
		return var.isAnonymous() && !var.hasValue() && var.isBNode();
	}
}
