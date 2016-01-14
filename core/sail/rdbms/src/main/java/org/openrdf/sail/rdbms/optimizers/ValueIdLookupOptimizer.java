/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.optimizers;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;
import org.openrdf.sail.rdbms.RdbmsValueFactory;

/**
 * Iterates through the query and converting the values into RDBMS values.
 * 
 * @author James Leigh
 * 
 */
public class ValueIdLookupOptimizer implements QueryOptimizer {

	RdbmsValueFactory vf;

	public ValueIdLookupOptimizer(RdbmsValueFactory vf) {
		super();
		this.vf = vf;
	}

	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new VarVisitor());
	}

	protected class VarVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meet(Var var) {
			if (var.hasValue()) {
				var.setValue(vf.asRdbmsValue(var.getValue()));
			}
		}
	}
}
