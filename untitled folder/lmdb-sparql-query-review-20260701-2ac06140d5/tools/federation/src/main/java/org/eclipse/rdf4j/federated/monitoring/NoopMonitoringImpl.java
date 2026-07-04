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

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

public class NoopMonitoringImpl implements Monitoring {

	public NoopMonitoringImpl() {
	}

	@Override
	public void monitorRemoteRequest(Endpoint e) {
	}

	@Override
	public void resetMonitoringInformation() {
	}

	@Override
	public void monitorQuery(QueryInfo query) {
	}

	@Override
	public void logQueryPlan(TupleExpr tupleExpr) {
	}

}
