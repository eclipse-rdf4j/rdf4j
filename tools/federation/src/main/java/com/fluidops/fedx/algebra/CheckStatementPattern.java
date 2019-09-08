/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.algebra;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.iterator.SingleBindingSetIteration;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * A statement pattern with no free variables when provided with some particular BindingSet
 * in evaluate. For evaluation a boolean ASK query is performed.
 *  
 * Wraps a StatementTupleExpr
 * 
 * @author Andreas Schwarte
 */
public class CheckStatementPattern implements StatementTupleExpr, BoundJoinTupleExpr {

	private static final long serialVersionUID = -4063951571744144255L;

	protected final StatementTupleExpr stmt;
	protected final String id;
	
	public CheckStatementPattern(StatementTupleExpr stmt) {
		super();
		this.stmt = stmt;
		this.id = NodeFactory.getNextId();
	}

	public StatementPattern getStatementPattern() {
		return (StatementPattern)stmt;
	}
	
	@Override
	public int getFreeVarCount() {
		return 0;
	}

	@Override
	public List<String> getFreeVars() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public List<StatementSource> getStatementSources() {
		return stmt.getStatementSources();
	}

	@Override
	public boolean hasFreeVarsFor(BindingSet binding) {
		return false;
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return stmt.getAssuredBindingNames();
	}

	@Override
	public Set<String> getBindingNames() {
		return stmt.getBindingNames();
	}

	@Override
	public QueryModelNode getParentNode() {
		return stmt.getParentNode();
	}

	@Override
	public String getSignature() {
		return stmt.getSignature();
	}

	@Override
	public void replaceChildNode(QueryModelNode current,
			QueryModelNode replacement) {
		stmt.replaceChildNode(current, replacement);
	}

	@Override
	public void replaceWith(QueryModelNode replacement) {
		stmt.replaceWith(replacement);		
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		stmt.setParentNode(parent);		
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		stmt.visit(visitor);		
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
			throws X {
		stmt.visitChildren(visitor);		
	}

	@Override
	public CheckStatementPattern clone() {
		throw new RuntimeException("Operation not supported on this node!");
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) throws QueryEvaluationException {
		
		StatementPattern st = (StatementPattern)stmt;
	
		try {
			// return true if at least one endpoint has a result for this binding set
			for (StatementSource source : stmt.getStatementSources()) {
				Endpoint ownedEndpoint = EndpointManager.getEndpointManager().getEndpoint(source.getEndpointID());
				TripleSource t = ownedEndpoint.getTripleSource();
				if (t.hasStatements(st, bindings))
					return new SingleBindingSetIteration(bindings);
			}
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
	
		// XXX return NULL instead and add an additional check?
		return new EmptyIteration<BindingSet, QueryEvaluationException>();
	}

	@Override
	public QueryInfo getQueryInfo() {
		return stmt.getQueryInfo();
	}
}
