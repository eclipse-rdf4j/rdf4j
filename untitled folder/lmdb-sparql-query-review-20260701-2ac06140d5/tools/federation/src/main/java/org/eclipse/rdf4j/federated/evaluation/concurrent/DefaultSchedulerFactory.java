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
 * The default {@link SchedulerFactory}
 */
public class DefaultSchedulerFactory implements SchedulerFactory {

	public static final DefaultSchedulerFactory INSTANCE = new DefaultSchedulerFactory();

	@Override
	public ControlledWorkerScheduler<BindingSet> createJoinScheduler(FederationContext federationContext,
			int nWorkers) {
		return new ControlledWorkerScheduler<>(nWorkers,
				"Join Scheduler");
	}

	@Override
	public ControlledWorkerScheduler<BindingSet> createUnionScheduler(FederationContext federationContext,
			int nWorkers) {
		return new ControlledWorkerScheduler<>(nWorkers,
				"Union Scheduler");
	}

	@Override
	public ControlledWorkerScheduler<BindingSet> createLeftJoinScheduler(FederationContext federationContext,
			int nWorkers) {
		return new ControlledWorkerScheduler<>(nWorkers,
				"Left Join Scheduler");
	}

}
