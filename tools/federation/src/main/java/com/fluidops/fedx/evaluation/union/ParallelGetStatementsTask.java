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
package com.fluidops.fedx.evaluation.union;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.evaluation.concurrent.ParallelTaskBase;

/**
 * A task implementation to retrieve statements for a given {@link StatementPattern}
 * using the provided triple source.
 * 
 * @author Andreas Schwarte
 */
public class ParallelGetStatementsTask extends ParallelTaskBase<Statement> {

	protected final ParallelExecutor<Statement> unionControl;
	protected final Endpoint endpoint;
	protected final Resource subj;

	protected final IRI pred;
	protected final Value obj;
	protected Resource[] contexts;
		
	public ParallelGetStatementsTask(ParallelExecutor<Statement> unionControl,
			Endpoint endpoint,
			Resource subj, IRI pred, Value obj, Resource... contexts)
	{
		super();
		this.unionControl = unionControl;
		this.endpoint = endpoint;
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.contexts = contexts;
		
	}
	
	@Override
	public ParallelExecutor<Statement> getControl() {
		return unionControl;
	}

	@Override
	public CloseableIteration<Statement, QueryEvaluationException> performTask()
			throws Exception {
		TripleSource tripleSource = endpoint.getTripleSource();
		return tripleSource.getStatements(subj, pred, obj, contexts);
	}
}
