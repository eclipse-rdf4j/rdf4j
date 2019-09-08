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

import org.eclipse.rdf4j.query.algebra.TupleExpr;

import com.fluidops.fedx.Config;


/**
 * Monitoring facility to maintain the query execution plan in
 * a variable local to the executing thread. Can be used to 
 * retrieve the query plan from the outside in the evaluation
 * thread.
 * 
 * This module is only active if {@link Config#isLogQueryPlan()}
 * is enabled. In addition {@link Config#isEnableMonitoring()} must
 * be set. In any other case, this class is a void operation.
 * 
 * @author Andreas Schwarte
 *
 */
public class QueryPlanLog
{

	static ThreadLocal<String> queryPlan = new ThreadLocal<String>();
	
	public static String getQueryPlan() {
		if (!isActive() || !Config.getConfig().isEnableMonitoring())
			throw new IllegalStateException("QueryPlan log module is not active, use monitoring.logQueryPlan=true in the configuration to activate.");
		return queryPlan.get();
	}
	
	public static void setQueryPlan(TupleExpr query) {
		if (!isActive())
			return;
		queryPlan.set(query.toString());
	}
	
	private static boolean isActive() {
		return Config.getConfig().isLogQueryPlan();
	}
	
}
