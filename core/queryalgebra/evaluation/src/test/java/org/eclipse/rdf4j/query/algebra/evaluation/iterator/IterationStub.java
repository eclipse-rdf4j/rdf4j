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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.io.Serializable;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

class IterationStub extends CloseableIteratorIteration<BindingSet, QueryEvaluationException> implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	int hasNextCount = 0;

	int nextCount = 0;

	int removeCount = 0;

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		hasNextCount++;
		return super.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		nextCount++;
		return super.next();
	}

	@Override
	public void remove() {
		removeCount++;
	}
}
