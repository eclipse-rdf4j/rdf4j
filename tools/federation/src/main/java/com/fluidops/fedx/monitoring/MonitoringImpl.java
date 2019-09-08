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
package com.fluidops.fedx.monitoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Implementation supporting the following monitoring features:
 * 
 * - monitor remote requests per endpoint
 * - maintain a query backlog using {@link QueryLog}
 * 
 * 
 * @author andreas_s
 *
 */
public class MonitoringImpl implements MonitoringService {

	/**
	 * Map endpoints to their request information
	 */
	private final Map<Endpoint, MonitoringInformation> requestMap = new ConcurrentHashMap<>();
	private final QueryLog queryLog;
	
	MonitoringImpl() {
		
		if (Config.getConfig().isLogQueries()) {
			try {
				queryLog = new QueryLog();
			} catch (IOException e) {
				throw new FedXRuntimeException("QueryLog cannot be initialized: " + e.getMessage());
			}
		} else
			queryLog=null;
	}

	
	@Override
	public void monitorRemoteRequest(Endpoint e) {
		MonitoringInformation m = requestMap.get(e);
		if (m==null) {
			m = new MonitoringInformation(e);
			requestMap.put(e, m);
		}
		m.increaseRequests();
	}	

	@Override
	public MonitoringInformation getMonitoringInformation(Endpoint e)
	{
		return requestMap.get(e);
	}


	@Override
	public List<MonitoringInformation> getAllMonitoringInformation()
	{
		return new ArrayList<MonitoringInformation>(requestMap.values());
	}


	@Override
	public void resetMonitoringInformation() {
		requestMap.clear();		
	}
	
	public static class MonitoringInformation { 
		private final Endpoint e;
		private int numberOfRequests = 0;
		public MonitoringInformation(Endpoint e){
			this.e = e;
		}
		private void increaseRequests() {
			// TODO make thread safe
			numberOfRequests++;
		}
		public String toString() {
			return e.getName() + " => " + numberOfRequests;
		}
		public Endpoint getE() {
			return e;
		}
		public int getNumberOfRequests(){
			return numberOfRequests;
		}		
	}

	@Override
	public void monitorQuery(QueryInfo query)	{
		if (queryLog!=null)
			queryLog.logQuery(query);		
	}

	@Override
	public void logQueryPlan(TupleExpr tupleExpr) {
		QueryPlanLog.setQueryPlan(tupleExpr);		
	}	
}
