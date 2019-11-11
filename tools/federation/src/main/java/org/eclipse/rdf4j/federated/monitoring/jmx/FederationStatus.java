/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.monitoring.jmx;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.query.BindingSet;

public class FederationStatus implements FederationStatusMBean {

	private final FederationContext federationContext;

	public FederationStatus(FederationContext federationContext) {
		super();
		this.federationContext = federationContext;
	}

	@Override
	public List<String> getFederationMembersDescription() {
		List<Endpoint> members = federationContext.getFederation().getMembers();
		List<String> res = new ArrayList<>();
		for (Endpoint e : members)
			res.add(e.toString());
		return res;
	}

	@Override
	public int getIdleJoinWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getJoinScheduler();
		return scheduler.getNumberOfIdleWorkers();
	}

	@Override
	public int getTotalJoinWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getJoinScheduler();
		return scheduler.getTotalNumberOfWorkers();
	}

	@Override
	public int getIdleUnionWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getUnionScheduler();
		return scheduler.getNumberOfIdleWorkers();
	}

	@Override
	public int getTotalUnionWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getUnionScheduler();
		return scheduler.getTotalNumberOfWorkers();
	}

	@Override
	public int getNumberOfScheduledJoinTasks() {
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getJoinScheduler();
		return scheduler.getNumberOfTasks();
	}

	@Override
	public int getNumberOfScheduledUnionTasks() {
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getUnionScheduler();
		return scheduler.getNumberOfTasks();
	}
}
