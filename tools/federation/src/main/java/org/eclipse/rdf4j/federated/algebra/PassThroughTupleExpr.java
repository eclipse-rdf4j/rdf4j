/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.FedXTupleQuery;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.google.common.collect.Lists;

/**
 * Marker {@link TupleExpr} that is used from {@link FedXTupleQuery#evaluate(TupleQueryResultHandler)} to allow for
 * passing through of results to the handler.
 * <p>
 * Passing through of results to the handler is supported for {@link SingleSourceQuery}s, i.e. if the original query is
 * sent as is to the single relevant source. In this case no materialization and in-memory handling through FedX is
 * done, if a {@link TupleQueryResultHandler} is supplied.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class PassThroughTupleExpr extends AbstractQueryModelNode implements FedXTupleExpr {

	private static final long serialVersionUID = 24797808099470499L;

	private final TupleExpr parsedQuery;

	private final TupleQueryResultHandler resultHandler;

	private boolean successfullyPassedThrough = false;

	public PassThroughTupleExpr(TupleExpr parsedQuery, TupleQueryResultHandler resultHandler) {
		super();
		this.parsedQuery = parsedQuery;
		this.resultHandler = resultHandler;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		parsedQuery.visit(visitor);

	}

	public TupleQueryResultHandler getResultHandler() {
		return resultHandler;
	}

	public TupleExpr getExpr() {
		return parsedQuery;
	}

	/**
	 *
	 * @return if the query result has already been passed through to the supplied {@link TupleQueryResultHandler}
	 */
	public boolean isPassedThrough() {
		return successfullyPassedThrough;
	}

	public void setPassedThrough(boolean flag) {
		this.successfullyPassedThrough = flag;
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
	public PassThroughTupleExpr clone() {
		return new PassThroughTupleExpr(parsedQuery, resultHandler);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(parsedQuery);
	}

	@Override
	public List<String> getFreeVars() {
		return Lists.newArrayList(QueryAlgebraUtil.getFreeVars(parsedQuery));
	}

	@Override
	public QueryInfo getQueryInfo() {
		throw new UnsupportedOperationException("Not supported to retrieve query info on this marker node");
	}
}
