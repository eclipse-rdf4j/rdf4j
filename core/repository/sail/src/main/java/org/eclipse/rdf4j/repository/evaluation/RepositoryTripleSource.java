/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.evaluation;

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

public class RepositoryTripleSource implements TripleSource {

	private final RepositoryConnection repo;

	private final boolean includeInferred;

	public RepositoryTripleSource(RepositoryConnection repo) {
		this(repo, true);
	}

	public RepositoryTripleSource(RepositoryConnection repo, boolean includeInferred) {
		this.repo = repo;
		this.includeInferred = includeInferred;
	}

	@Override
	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException {
		CloseableIteration<? extends Statement, RepositoryException> iter = null;
		CloseableIteration<? extends Statement, QueryEvaluationException> result = null;

		boolean allGood = false;
		try {
			iter = repo.getStatements(subj, pred, obj, includeInferred, contexts);
			result = new ExceptionConvertingIteration<Statement, QueryEvaluationException>(iter) {

				@Override
				protected QueryEvaluationException convert(Exception exception) {
					return new QueryEvaluationException(exception);
				}
			};
			allGood = true;
			return result;
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} finally {
			if (!allGood) {
				try {
					if (result != null) {
						result.close();
					}
				} finally {
					if (iter != null) {
						iter.close();
					}
				}
			}
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return repo.getValueFactory();
	}
}
