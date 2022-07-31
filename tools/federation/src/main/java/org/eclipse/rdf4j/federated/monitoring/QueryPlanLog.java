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
package org.eclipse.rdf4j.federated.monitoring;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Monitoring facility to maintain the query execution plan in a variable local to the executing thread. Can be used to
 * retrieve the query plan from the outside in the evaluation thread.
 *
 * This module is only active if {@link FedXConfig#isLogQueryPlan()} is enabled. In addition
 * {@link FedXConfig#isEnableMonitoring()} must be set. In any other case, this class is a void operation.
 *
 * @author Andreas Schwarte
 *
 */
public class QueryPlanLog {

	static ThreadLocal<String> queryPlan = new ThreadLocal<>();

	public static String getQueryPlan() {
		return queryPlan.get();
	}

	public static void setQueryPlan(TupleExpr query) {
		queryPlan.set(query.toString());
	}
}
