/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Converts Statement iteration (i.e. RepositoryResult) into the corresponding binding set. Note that exceptions are
 * converted appropriately as well.
 *
 * @author Andreas Schwarte
 */
public class StatementConversionIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	protected final RepositoryResult<Statement> repoResult;
	protected final BindingSet bindings;
	protected final StatementPattern stmt;

	protected boolean updateSubj = false;
	protected boolean updatePred = false;
	protected boolean updateObj = false;
	protected boolean updateContext = false;

	public StatementConversionIteration(RepositoryResult<Statement> repoResult,
			BindingSet bindings, StatementPattern stmt) {
		super();
		this.repoResult = repoResult;
		this.bindings = bindings;
		this.stmt = stmt;
		init();
	}

	protected void init() {
		updateSubj = stmt.getSubjectVar() != null && !bindings.hasBinding(stmt.getSubjectVar().getName());
		updatePred = stmt.getPredicateVar() != null && !bindings.hasBinding(stmt.getPredicateVar().getName());
		updateObj = stmt.getObjectVar() != null && !bindings.hasBinding(stmt.getObjectVar().getName());
		updateContext = stmt.getContextVar() != null && !bindings.hasBinding(stmt.getContextVar().getName());
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		try {
			return repoResult.hasNext();
		} catch (RepositoryException e) {
			throw convertException(e);
		}
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {

		try {
			return convert(repoResult.next());
		} catch (NoSuchElementException | IllegalStateException e) {
			throw e;
		} catch (RepositoryException e) {
			throw convertException(e);
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {

		try {
			repoResult.remove();
		} catch (UnsupportedOperationException | IllegalStateException e) {
			throw e;
		} catch (RepositoryException e) {
			throw convertException(e);
		}

	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			repoResult.close();
		}
	}

	protected BindingSet convert(Statement st) {
		QueryBindingSet result = new QueryBindingSet(bindings);

		if (updateSubj) {
			result.addBinding(stmt.getSubjectVar().getName(), st.getSubject());
		}
		if (updatePred) {
			result.addBinding(stmt.getPredicateVar().getName(), st.getPredicate());
		}
		if (updateObj) {
			result.addBinding(stmt.getObjectVar().getName(), st.getObject());
		}
		if (updateContext && st.getContext() != null) {
			result.addBinding(stmt.getContextVar().getName(), st.getContext());
		}

		return result;
	}

	protected QueryEvaluationException convertException(Exception e) {
		return new QueryEvaluationException(e);
	}

}
