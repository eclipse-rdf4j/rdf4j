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

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapping iteration that attempts to close the dependent
 * {@link RepositoryConnection} after consumption.
 * 
 * @author Andreas Schwarte
 *
 */
public class CloseDependentConnectionIteration<T>
		extends AbstractCloseableIteration<T, QueryEvaluationException> {

	private static final Logger log = LoggerFactory.getLogger(CloseDependentConnectionIteration.class);

	protected final CloseableIteration<T, QueryEvaluationException> inner;
	protected final RepositoryConnection dependentConn;

	public CloseDependentConnectionIteration(
			CloseableIteration<T, QueryEvaluationException> inner,
			RepositoryConnection dependentConn) {
		super();
		this.inner = inner;
		this.dependentConn = dependentConn;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		boolean res = inner.hasNext();
		if (!res) {
			try {
				dependentConn.close();
			} catch (Throwable ignore) {
				log.trace("Failed to close dependent connection:", ignore);
			}
		}
		return res;
	}

	@Override
	public T next() throws QueryEvaluationException {
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
			try {
				super.handleClose();
			} finally {
				dependentConn.close();
			}
		}
	}

}
