/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerBindJoin;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerBindLeftJoin;
import org.eclipse.rdf4j.federated.evaluation.join.ParallelBindLeftJoinTask;
import org.eclipse.rdf4j.federated.evaluation.join.ParallelBoundJoinTask;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * Factory for creating {@link ControlledWorkerScheduler} for executing subqueries (e.g. joins) in the background
 *
 * @see DefaultSchedulerFactory
 * @author Andreas Schwarte
 */
public interface SchedulerFactory {

	/**
	 * Create a {@link ControlledWorkerScheduler} for regular joins (e.g., the sub-queries generated as part of bind
	 * joins)
	 *
	 * @param federationContext
	 * @param nWorkers
	 * @return
	 * @see ControlledWorkerBindJoin
	 * @see ParallelBoundJoinTask
	 */
	ControlledWorkerScheduler<BindingSet> createJoinScheduler(FederationContext federationContext, int nWorkers);

	/**
	 * Create a {@link ControlledWorkerScheduler} for unions (e.g., for executing UNION operands in parallel)
	 *
	 * @param federationContext
	 * @param nWorkers
	 * @return
	 */
	ControlledWorkerScheduler<BindingSet> createUnionScheduler(FederationContext federationContext, int nWorkers);

	/**
	 * Create a {@link ControlledWorkerScheduler} for left joins (e.g., the sub-queries generated as part of left bind
	 * joins, i.e. OPTIONAL)
	 *
	 * @param federationContext
	 * @param nWorkers
	 * @return
	 * @see ControlledWorkerBindLeftJoin
	 * @see ParallelBindLeftJoinTask
	 */
	ControlledWorkerScheduler<BindingSet> createLeftJoinScheduler(FederationContext federationContext, int nWorkers);
}
