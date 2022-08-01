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

import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Convenience iteration to convert {@link RepositoryResult} exceptions to {@link QueryEvaluationException}.
 *
 * @author Andreas Schwarte
 *
 * @param <T>
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class RepositoryExceptionConvertingIteration<T>
		extends ExceptionConvertingIteration<T, QueryEvaluationException> {

	public RepositoryExceptionConvertingIteration(
			Iteration<? extends T, ? extends Exception> iter) {
		super(iter);
	}

	@Override
	protected QueryEvaluationException convert(Exception e) {
		return new QueryEvaluationException(e);
	}
}
