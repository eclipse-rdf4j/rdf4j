/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.IteratingGraphQueryResult;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Arjohn Kampman
 */
public class SailGraphQuery extends SailQuery implements GraphQuery {

	protected SailGraphQuery(ParsedGraphQuery tupleQuery, SailRepositoryConnection con) {
		super(tupleQuery, con);
	}

	@Override
	public ParsedGraphQuery getParsedQuery() {
		return (ParsedGraphQuery)super.getParsedQuery();
	}

	public GraphQueryResult evaluate()
		throws QueryEvaluationException
	{
		TupleExpr tupleExpr = getParsedQuery().getTupleExpr();

		try {
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter;
			
			SailConnection sailCon = getConnection().getSailConnection();
			bindingsIter = sailCon.evaluate(tupleExpr, getActiveDataset(), getBindings(), getIncludeInferred());

			// Filters out all partial and invalid matches
			bindingsIter = new FilterIteration<BindingSet, QueryEvaluationException>(bindingsIter) {

				@Override
				protected boolean accept(BindingSet bindingSet) {
					Value context = bindingSet.getValue("context");

					return bindingSet.getValue("subject") instanceof Resource
							&& bindingSet.getValue("predicate") instanceof IRI
							&& bindingSet.getValue("object") instanceof Value
							&& (context == null || context instanceof Resource);
				}
			};
			
			bindingsIter = enforceMaxQueryTime(bindingsIter);

			// Convert the BindingSet objects to actual RDF statements
			final ValueFactory vf = getConnection().getRepository().getValueFactory();
			CloseableIteration<Statement, QueryEvaluationException> stIter;
			stIter = new ConvertingIteration<BindingSet, Statement, QueryEvaluationException>(bindingsIter) {

				@Override
				protected Statement convert(BindingSet bindingSet) {
					Resource subject = (Resource)bindingSet.getValue("subject");
					IRI predicate = (IRI)bindingSet.getValue("predicate");
					Value object = bindingSet.getValue("object");
					Resource context = (Resource)bindingSet.getValue("context");

					if (context == null) {
						return vf.createStatement(subject, predicate, object);
					}
					else {
						return vf.createStatement(subject, predicate, object, context);
					}
				}
			};

			return new IteratingGraphQueryResult(getParsedQuery().getQueryNamespaces(), stIter);
		}
		catch (SailException e) {
			throw new QueryEvaluationException(e.getMessage(), e);
		}
	}

	public void evaluate(RDFHandler handler)
		throws QueryEvaluationException, RDFHandlerException
	{
		GraphQueryResult queryResult = evaluate();
		QueryResults.report(queryResult, handler);
	}
}
