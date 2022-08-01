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
package org.eclipse.rdf4j.federated.evaluation.union;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutorBase;
import org.eclipse.rdf4j.federated.structures.QueryInfo;

/**
 * Base class for any parallel union executor.
 *
 * Note that this class extends {@link LookAheadIteration} and thus any implementation of this class is applicable for
 * pipelining when used in a different thread (access to shared variables is synchronized).
 *
 * @author Andreas Schwarte
 *
 */
public abstract class UnionExecutorBase<T> extends ParallelExecutorBase<T> {

	public UnionExecutorBase(QueryInfo queryInfo) {
		super(queryInfo);
	}

	@Override
	protected final void performExecution() throws Exception {
		union();
	}

	/**
	 *
	 * Note: this method must block until the union is executed completely. Otherwise the result queue is marked as
	 * committed while this isn't the case. The blocking behavior in general is no problem: If you need concurrent
	 * access to the result (i.e. pipelining) just run the union in a separate thread. Access to the result iteration is
	 * synchronized.
	 *
	 * @throws Exception
	 */
	protected abstract void union() throws Exception;

	@Override
	protected String getExecutorType() {
		return "Union";
	}

}
