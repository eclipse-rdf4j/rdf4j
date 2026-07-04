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
package org.eclipse.rdf4j.federated.algebra;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.google.common.collect.Lists;

/**
 * An {@link ArbitraryLengthPath} node which can be evaluated at a single node.
 *
 * @author Andreas Schwarte
 *
 */
public class ExclusiveArbitraryLengthPath extends ArbitraryLengthPath
		implements ExclusiveTupleExprRenderer {

	private static final long serialVersionUID = 5743134085306940200L;

	private final StatementSource owner;

	private final QueryInfo queryInfo;

	private final List<String> freeVars;

	public ExclusiveArbitraryLengthPath(ArbitraryLengthPath path, StatementSource owner, QueryInfo queryInfo) {
		super(path.getScope(), path.getSubjectVar().clone(), path.getPathExpression(), path.getObjectVar().clone(),
				path.getContextVar() != null ? path.getContextVar().clone() : null, path.getMinLength());
		this.owner = owner;
		this.queryInfo = queryInfo;
		this.freeVars = computeFreeVars();
	}

	private List<String> computeFreeVars() {
		if (this.getPathExpression() instanceof StatementTupleExpr) {
			return ((StatementTupleExpr) this.getPathExpression()).getFreeVars();
		}
		List<String> freeVars = Lists.newArrayList();
		if (!getSubjectVar().hasValue()) {
			freeVars.add(getSubjectVar().getName());
		}
		if (!getObjectVar().hasValue()) {
			freeVars.add(getObjectVar().getName());
		}
		return freeVars;
	}

	@Override
	public StatementSource getOwner() {
		return owner;
	}

	@Override
	public ExclusiveArbitraryLengthPath clone() {
		return new ExclusiveArbitraryLengthPath(super.clone(), owner, queryInfo);
	}

	@Override
	public String toQueryString(Set<String> varNames, BindingSet bindings) {
		return QueryStringUtil.toString(this, varNames, bindings);
	}

	@Override
	public TupleExpr toQueryAlgebra(Set<String> varNames, BindingSet bindings) {
		return QueryAlgebraUtil.toTupleExpr(this, varNames, bindings);
	}

	@Override
	public List<String> getFreeVars() {
		return freeVars;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return this.queryInfo;
	}
}
