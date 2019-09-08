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

import org.eclipse.rdf4j.federated.FederationManager;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.query.BindingSet;


public class FederationStatus implements FederationStatusMBean {

	@Override
	public List<String> getFederationMembersDescription() {
		List<Endpoint> members = FederationManager.getInstance().getFederation().getMembers();
		List<String> res = new ArrayList<String>();		
		for (Endpoint e : members) 
			res.add(e.toString());
		return res;
	}

	@Override
	public int getIdleJoinWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getJoinScheduler();
		return scheduler.getNumberOfIdleWorkers();
	}

	@Override
	public int getTotalJoinWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getJoinScheduler();
		return scheduler.getTotalNumberOfWorkers();
	}

	@Override
	public int getIdleUnionWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getUnionScheduler();
		return scheduler.getNumberOfIdleWorkers();
	}

	@Override
	public int getTotalUnionWorkerThreads() {
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getUnionScheduler();
		return scheduler.getTotalNumberOfWorkers();
	}

	@Override
	public int getNumberOfScheduledJoinTasks() {
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getJoinScheduler();
		return scheduler.getNumberOfTasks();
	}

	@Override
	public int getNumberOfScheduledUnionTasks() {
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getUnionScheduler();
		return scheduler.getNumberOfTasks();
	}
}
