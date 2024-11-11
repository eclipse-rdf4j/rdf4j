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
import org.eclipse.rdf4j.query.BindingSet;

/**
 * Factory for creating {@link ControlledWorkerScheduler} for executing subqueries (e.g. joins) in the background
 *
 * @see DefaultSchedulerFactory
 * @author Andreas Schwarte
 */
public interface SchedulerFactory {

	/**
	 * Create a {@link ControlledWorkerScheduler} for joins
	 *
	 * @param federationContext
	 * @param nWorkers
	 * @return
	 */
	ControlledWorkerScheduler<BindingSet> createJoinScheduler(FederationContext federationContext, int nWorkers);

	/**
	 * Create a {@link ControlledWorkerScheduler} for unions
	 *
	 * @param federationContext
	 * @param nWorkers
	 * @return
	 */
	ControlledWorkerScheduler<BindingSet> createUnionScheduler(FederationContext federationContext, int nWorkers);

	/**
	 * Create a {@link ControlledWorkerScheduler} for left joins
	 *
	 * @param federationContext
	 * @param nWorkers
	 * @return
	 */
	ControlledWorkerScheduler<BindingSet> createLeftJoinScheduler(FederationContext federationContext, int nWorkers);
}
