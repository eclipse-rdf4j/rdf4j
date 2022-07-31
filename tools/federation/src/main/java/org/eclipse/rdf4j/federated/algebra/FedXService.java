/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.Collection;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public class FedXService extends AbstractQueryModelNode implements TupleExpr, BoundJoinTupleExpr {

	private static final long serialVersionUID = 7179501550561942879L;

	protected Service expr;
	protected transient QueryInfo queryInfo;
	protected boolean simple = true; // consists of BGPs only
	protected int nTriples = 0;

	public FedXService(Service expr, QueryInfo queryInfo) {
		this.expr = expr;
		this.queryInfo = queryInfo;
		expr.visit(new ServiceAnalyzer());
	}

	public Service getService() {
		return this.expr;
	}

	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

	public int getNumberOfTriplePatterns() {
		return nTriples;
	}

	public boolean isSimple() {
		return simple;
	}

	public Collection<String> getFreeVars() {
		return expr.getServiceVars();
	}

	public int getFreeVarCount() {
		return expr.getServiceVars().size();
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		expr.visit(visitor);
	}

	@Override
	public FedXService clone() {
		return (FedXService) super.clone();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return expr.getAssuredBindingNames();
	}

	@Override
	public Set<String> getBindingNames() {
		return expr.getBindingNames();
	}

	private class ServiceAnalyzer extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		protected void meetNode(QueryModelNode node) {
			if (node instanceof StatementTupleExpr) {
				nTriples++;
			} else if (node instanceof StatementPattern) {
				nTriples++;
			} else if (node instanceof Filter) {
				simple = false;
			} else if (node instanceof Union) {
				simple = false;
			}

			super.meetNode(node);
		}

	}
}
