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
package com.fluidops.fedx.evaluation.iterator;

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
 * Converts Statement iteration (i.e. RepositoryResult) into the corresponding binding set. Note that
 * exceptions are converted appropriately as well.
 * 
 * @author Andreas Schwarte
 */
public class StatementConversionIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException>
{

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
		} catch(NoSuchElementException e) {
			throw e;
	    } catch(IllegalStateException e) {
	    	throw e;
	    } catch (RepositoryException e) {
			throw convertException(e);
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {

		try {
			repoResult.remove();
		} catch(UnsupportedOperationException e) {
			throw e;
		} catch(IllegalStateException e) {
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
