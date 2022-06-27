/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.FedXLeftJoin;
import org.eclipse.rdf4j.federated.algebra.FederatedDescribeOperator;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * Generic optimizer
 *
 * Tasks: - Collect information (hasUnion, hasFilter, hasService) - Collect all statements in a list (for source
 * selection), do not collect SERVICE expressions - Collect all Join arguments and group them in the NJoin structure for
 * easier optimization (flatten)
 *
 * @author Andreas Schwarte
 */
public class GenericInfoOptimizer extends AbstractSimpleQueryModelVisitor<OptimizationException>
		implements FedXOptimizer {

	protected boolean hasFilter = false;
	protected boolean hasUnion = false;
	protected List<Service> services = null;
	protected long limit = -1; // set to a positive number if the main query has a limit
	protected List<StatementPattern> stmts = new ArrayList<>();

	// internal helpers
	private boolean seenProjection = false; // whether the main projection has been visited

	protected final QueryInfo queryInfo;

	public GenericInfoOptimizer(QueryInfo queryInfo) {
		super(true);
		this.queryInfo = queryInfo;
	}

	public boolean hasFilter() {
		return hasFilter;
	}

	public boolean hasUnion() {
		return hasUnion;
	}

	public List<StatementPattern> getStatements() {
		return stmts;
	}

	public boolean hasLimit() {
		return limit > 0;
	}

	public long getLimit() {
		return limit;
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {

		try {
			tupleExpr.visit(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void meet(Union union) {
		hasUnion = true;
		super.meet(union);
	}

	@Override
	public void meet(Filter filter) {
		hasFilter = true;
		super.meet(filter);
	}

	@Override
	public void meet(Service service) {
		if (services == null) {
			services = new ArrayList<>();
		}
		services.add(service);
	}

	@Override
	public void meet(Join node) {

		/*
		 * Optimization task:
		 *
		 * Collect all join arguments recursively and create the NJoin structure for easier join order optimization
		 */

		NJoin newJoin = OptimizerUtil.flattenJoin(node, queryInfo);
		newJoin.visitChildren(this);

		node.replaceWith(newJoin);
	}

	@Override
	public void meet(LeftJoin node) throws OptimizationException {
		/**
		 * Wrap the left join in order to keep a reference to the query info object
		 */
		FedXLeftJoin join = new FedXLeftJoin(node, queryInfo);
		join.visitChildren(this);

		node.replaceWith(join);
	}

	@Override
	public void meet(StatementPattern node) {
		stmts.add(node);
	}

	@Override
	public void meet(Projection node) throws OptimizationException {
		seenProjection = true;
		super.meet(node);
	}

	@Override
	public void meet(Slice node) throws OptimizationException {
		// remember the limit of the main query (i.e. outside of a projection)
		if (!seenProjection) {
			limit = node.getLimit();
		}
		super.meet(node);
	}

	@Override
	public void meet(DescribeOperator node) throws OptimizationException {
		/*
		 * Replace with a FedX Describe Operator
		 */
		FederatedDescribeOperator newNode = new FederatedDescribeOperator(node.getArg(), queryInfo);
		newNode.visitChildren(this);

		node.replaceWith(newNode);
	}

	public boolean hasService() {
		return services != null && services.size() > 0;
	}

	public List<Service> getServices() {
		return services == null ? Collections.emptyList() : services;
	}
}
