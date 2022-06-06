/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.Set;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A query which has a single relevant source. These queries can be sent entirely to the endpoint as-is.
 *
 * @author Andreas Schwarte
 */
public class SingleSourceQuery extends AbstractQueryModelNode implements TupleExpr, QueryRef {
	private static final long serialVersionUID = 5745172129911897271L;

	private final TupleExpr parsedQuery;
	private final transient Endpoint source;
	private final transient QueryInfo queryInfo;

	/**
	 * @param parsedQuery
	 * @param source
	 * @param queryInfo
	 */
	public SingleSourceQuery(TupleExpr parsedQuery, Endpoint source,
			QueryInfo queryInfo) {
		super();
		this.parsedQuery = parsedQuery;
		this.source = source;
		this.queryInfo = queryInfo;
	}

	public Endpoint getSource() {
		return source;
	}

	public String getQueryString() {
		return queryInfo.getQuery();
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		parsedQuery.visit(visitor);
	}

	@Override
	public String getSignature() {
		return super.getSignature() + " @" + source.getId();
	}

	@Override
	public Set<String> getBindingNames() {
		return parsedQuery.getBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return parsedQuery.getAssuredBindingNames();
	}

	@Override
	public SingleSourceQuery clone() {
		return (SingleSourceQuery) super.clone();
	}

}
