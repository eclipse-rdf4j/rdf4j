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

import java.util.LinkedList;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Print the bindings of the inner iteration to stdout, however maintain a copy, which is accessible through this
 * iteration.
 *
 * @author Andreas Schwarte
 *
 */
public class PrintingIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException> {

	protected final CloseableIteration<BindingSet, QueryEvaluationException> inner;
	protected LinkedList<BindingSet> copyQueue = new LinkedList<>();
	protected boolean done = false;

	public PrintingIteration(
			CloseableIteration<BindingSet, QueryEvaluationException> inner) {
		super();
		this.inner = inner;
	}

	public void print() throws QueryEvaluationException {
		int count = 0;
		while (inner.hasNext()) {
			BindingSet item = inner.next();
			System.out.println(item);
			count++;
			synchronized (copyQueue) {
				copyQueue.addLast(item);
			}
		}
		done = true;
		System.out.println("Done with inner queue. Processed " + count + " items.");
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return !done || copyQueue.size() > 0;
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		synchronized (copyQueue) {
			return copyQueue.removeFirst();
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		inner.close();
		done = true;
		synchronized (copyQueue) {
			copyQueue.clear();
		}
	}
}
