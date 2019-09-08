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
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

import com.fluidops.fedx.algebra.FilterValueExpr;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.evaluation.concurrent.ParallelTaskBase;
import com.fluidops.fedx.util.QueryStringUtil;

/**
 * A task implementation representing a statement expression to be evaluated.
 * 
 * @author Andreas Schwarte
 */
public class ParallelUnionTask extends ParallelTaskBase<BindingSet> {
	
	protected final Endpoint endpoint;
	protected final StatementPattern stmt;
	protected final BindingSet bindings;
	protected final ParallelExecutor<BindingSet> unionControl;
	protected final FilterValueExpr filterExpr;
	
	public ParallelUnionTask(ParallelExecutor<BindingSet> unionControl, StatementPattern stmt, Endpoint endpoint,
			BindingSet bindings, FilterValueExpr filterExpr) {
		this.endpoint = endpoint;
		this.stmt = stmt;
		this.bindings = bindings;
		this.unionControl = unionControl;
		this.filterExpr = filterExpr;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask() throws Exception {
		TripleSource tripleSource = endpoint.getTripleSource();
		return tripleSource.getStatements(stmt, bindings, filterExpr);
	}

	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return unionControl;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " @" + endpoint.getId() + ": " + QueryStringUtil.toString(stmt);
	}
}
