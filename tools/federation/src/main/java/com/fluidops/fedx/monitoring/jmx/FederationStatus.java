/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.monitoring.jmx;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;

import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.concurrent.ControlledWorkerScheduler;


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
