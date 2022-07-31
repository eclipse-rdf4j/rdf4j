/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.concurrent.Phaser;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * A delegating {@link ParallelExecutor} which arrives and de-registers on the phaser on completion of a task.
 *
 * @author Andreas Schwarte
 *
 */
class PhaserHandlingParallelExecutor implements ParallelExecutor<BindingSet> {

	private final ParallelExecutor<BindingSet> delegate;
	private final Phaser phaser;

	public PhaserHandlingParallelExecutor(ParallelExecutor<BindingSet> delegate, Phaser phaser) {
		super();
		this.delegate = delegate;
		this.phaser = phaser;
	}

	@Override
	public void addResult(CloseableIteration<BindingSet, QueryEvaluationException> res) {
		delegate.addResult(res);
	}

	@Override
	public void toss(Exception e) {
		phaser.arriveAndDeregister();
		delegate.toss(e);
	}

	@Override
	public void done() {
		phaser.arriveAndDeregister();
		delegate.done();
	}

	@Override
	public boolean isFinished() {
		return delegate.isFinished();
	}

	@Override
	public QueryInfo getQueryInfo() {
		return delegate.getQueryInfo();
	}

	@Override
	public void run() {
		delegate.run();
	}
}
