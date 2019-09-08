/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.monitoring;

import org.eclipse.rdf4j.federated.Config;
import org.eclipse.rdf4j.query.algebra.TupleExpr;


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
