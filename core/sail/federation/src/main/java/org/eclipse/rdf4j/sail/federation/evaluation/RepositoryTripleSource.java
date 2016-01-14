/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Allow a single repository member to control a EvaulationStrategy.
 * 
 * @author James Leigh
 */
public class RepositoryTripleSource implements TripleSource {

	private final RepositoryConnection repo;

	public RepositoryTripleSource(RepositoryConnection repo) {
		this.repo = repo;
	}

	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
			IRI pred, Value obj, Resource... contexts)
		throws QueryEvaluationException
	{
		RepositoryResult<Statement> result;
		try {
			result = repo.getStatements(subj, pred, obj, true, contexts);
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		}
		return new ExceptionConvertingIteration<Statement, QueryEvaluationException>(result) {

			@Override
			protected QueryEvaluationException convert(Exception exception) {
				return new QueryEvaluationException(exception);
			}
		};
	}

	public ValueFactory getValueFactory() {
		return repo.getValueFactory();
	}

}
