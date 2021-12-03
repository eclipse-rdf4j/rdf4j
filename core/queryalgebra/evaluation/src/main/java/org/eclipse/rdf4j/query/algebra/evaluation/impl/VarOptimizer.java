/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Optimizes a query model by inlining {@link BindingSetAssignment} values where possible.
 *
 * @author Jeen Broekstra
 */
public class VarOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		VarVisitor visitor = new VarVisitor();
		tupleExpr.visit(visitor);
	}

	private static class VarVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final HashMap<String, String> vars = new HashMap<>();

		@Override
		public void meet(Var var) {
			var.setName(intern(var.getName()));
		}

		@Override
		public void meet(ProjectionElem elem) {
			elem.setSourceName(intern(elem.getSourceName()));
			elem.setTargetName(intern(elem.getTargetName()));
		}

		private String intern(String sourceName) {
			String s = vars.get(sourceName);
			if (s == null) {
				vars.put(sourceName, sourceName);
				s = sourceName;
			}
			return s;
		}

	}
}
