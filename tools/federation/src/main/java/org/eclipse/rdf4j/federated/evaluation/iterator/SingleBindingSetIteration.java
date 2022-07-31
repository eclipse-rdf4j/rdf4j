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

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Represents an iteration that contains only a single binding set.
 *
 * @author Andreas Schwarte
 *
 */
public class SingleBindingSetIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	protected final BindingSet res;
	protected boolean hasNext = true;

	public SingleBindingSetIteration(BindingSet res) {
		super();
		this.res = res;
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public BindingSet next() {
		hasNext = false;
		return res;
	}

	@Override
	public void remove() {
		// no-op
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		hasNext = false;
	}
}
