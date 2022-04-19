/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A tuple expression that represents an nary-Join.
 *
 * @author Andreas Schwarte
 *
 */
public class NJoin extends NTuple {

	private static final long serialVersionUID = -8646701006458860154L;

	/**
	 * Construct an nary-tuple. Note that the parentNode of all arguments is set to this instance.
	 *
	 * @param args
	 */
	public NJoin(List<TupleExpr> args, QueryInfo queryInfo) {
		super(args, queryInfo);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public NJoin clone() {
		return (NJoin) super.clone();
	}

	/**
	 * Returns the commons variables of the join with the given index.
	 *
	 * @param joinIndex the join index, starting with 1
	 * @return the set of join variables
	 */
	public Set<String> getJoinVariables(int joinIndex) {

		Set<String> joinVars = new HashSet<>();
		joinVars.addAll(QueryAlgebraUtil.getFreeVars(getArg(joinIndex - 1)));
		joinVars.retainAll(QueryAlgebraUtil.getFreeVars(getArg(joinIndex)));
		return joinVars;
	}
}
