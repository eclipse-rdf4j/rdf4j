/*
 * Copyright (C) 2019 Veritas Technologies LLC.
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

import java.util.concurrent.Future;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.fluidops.fedx.evaluation.concurrent.ParallelTask;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * A wrapping iteration that attempts to close all running scheduled
 * {@link Future}s for the given query evaluation.
 * <p>
 * This is required for instance if the resulting iteration is not fully
 * consumed.
 * </p>
 * 
 * @author Andreas Schwarte
 * @see QueryInfo#close()
 * @see ParallelTask#cancel()
 *
 */
public class StopRemainingExecutionsOnCloseIteration
		extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	protected final CloseableIteration<? extends BindingSet, QueryEvaluationException> inner;
	protected final QueryInfo queryInfo;

	public StopRemainingExecutionsOnCloseIteration(
			CloseableIteration<? extends BindingSet, QueryEvaluationException> inner, QueryInfo queryInfo) {
		super();
		this.inner = inner;
		this.queryInfo = queryInfo;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return inner.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		return inner.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		inner.remove();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			inner.close();
		} finally {
			super.handleClose();
			// make sure to close all scheduled / running parallel executions
			// (e.g. if the query result is not fully consumed)
			queryInfo.close();
		}
	}

}
