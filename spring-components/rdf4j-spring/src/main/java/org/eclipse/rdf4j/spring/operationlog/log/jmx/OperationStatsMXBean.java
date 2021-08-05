/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.operationlog.log.jmx;

import java.util.List;

public interface OperationStatsMXBean {
	void reset();

	int getDistinctOperationCount();

	int getDistinctOperationExecutionCount();

	int getTotalOperationExecutionCount();

	int getTotalFailedOperationExecutionCount();

	long getTotalOperationExecutionTime();

	List<AggregatedOperationStats> getAggregatedOperationStats();
}