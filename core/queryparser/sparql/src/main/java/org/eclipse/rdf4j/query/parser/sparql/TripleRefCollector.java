/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

// constructs a map from variable name to TripleRef or ValueExprTripleRef for processing extensions
public class TripleRefCollector extends AbstractQueryModelVisitor<RuntimeException> {

	public static Map<String, Object> process(QueryModelNode node) {
		TripleRefCollector collector = new TripleRefCollector();
		node.visit(collector);
		return collector.getTripleRefs();
	}

	private final Map<String, Object> tripleRefs = new HashMap<>();

	public Map<String, Object> getTripleRefs() {
		return tripleRefs;
	}

	@Override
	public void meet(Filter node) {
		// Skip boolean constraints
		node.getArg().visit(this);
	}

	@Override
	public void meet(TripleRef node) {
		tripleRefs.put(node.getExprVar().getName(), node);
	}

	@Override
	public void meet(ValueExprTripleRef node) {
		tripleRefs.put(node.getExtVarName(), node);
	}

	@Override
	public void meetOther(QueryModelNode node) {
		if (node instanceof TripleRef) {
			tripleRefs.put(((TripleRef) node).getExprVar().getName(), node);
		} else if (node instanceof ValueExprTripleRef) {
			tripleRefs.put(((ValueExprTripleRef) node).getExtVarName(), node);
		} else {
			super.meetOther(node);
		}
	}
}
