/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.algebra;

import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.federation.evaluation.InsertBindingSetCursor;

/**
 * Indicates that the argument should be evaluated in a particular member.
 * 
 * @author James Leigh
 */
public class OwnedTupleExpr extends UnaryTupleOperator {

	private final RepositoryConnection owner;

	private TupleQuery query;

	private Map<String, String> variables;

	public OwnedTupleExpr(RepositoryConnection owner, TupleExpr arg) {
		super(arg);
		this.owner = owner;
	}

	public RepositoryConnection getOwner() {
		return owner;
	}

	public void prepare(QueryLanguage queryLn, String qry, Map<String, String> bindings)
		throws RepositoryException, MalformedQueryException
	{
		assert this.query == null;
		this.query = owner.prepareTupleQuery(queryLn, qry);
		this.variables = bindings;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Dataset dataset,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		CloseableIteration<BindingSet, QueryEvaluationException> rval = null;
		if (query != null) {
			try {
				synchronized (query) {
					for (String name : variables.keySet()) {
						if (bindings.hasBinding(name)) {
							Value value = bindings.getValue(name);
							query.setBinding(variables.get(name), value);
						}
						else {
							query.removeBinding(variables.get(name));
						}
					}
					query.setDataset(dataset);
					TupleQueryResult result = query.evaluate();
					rval = new InsertBindingSetCursor(result, bindings);
				}
			}
			catch (IllegalArgumentException e) { // NOPMD
				// query does not support BNode bindings
			}
		}
		return rval;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meetOther(this);
	}

	@Override
	public String getSignature() {
		return this.getClass().getSimpleName() + " " + owner.toString();
	}

}
