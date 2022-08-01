/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog.log.jmx;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.spring.dao.exception.RDF4JSpringException;
import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStats;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class AggregatedOperationStats implements Cloneable {
	private String operation = null;
	private int count = 0;
	private int failed = 0;
	private long cumulativeTime = 0;
	private Integer uniqueBindingsCount = null;
	private Set<Integer> bindingsHashcodes = new HashSet<>();

	public AggregatedOperationStats() {
	}

	public static AggregatedOperationStats build(OperationExecutionStats stats) {
		return new AggregatedOperationStats().buildNext(stats);
	}

	@Override
	protected Object clone() {
		AggregatedOperationStats theClone;
		try {
			theClone = (AggregatedOperationStats) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RDF4JSpringException("could not clone", e);
		}
		theClone.operation = this.operation;
		theClone.count = this.count;
		theClone.failed = this.failed;
		theClone.cumulativeTime = this.cumulativeTime;
		theClone.uniqueBindingsCount = this.uniqueBindingsCount;
		theClone.bindingsHashcodes = new HashSet<>();
		theClone.bindingsHashcodes.addAll(this.bindingsHashcodes);
		return theClone;
	}

	public void setUniqueBindingsCount(int uniqueBindingsCount) {
		this.uniqueBindingsCount = uniqueBindingsCount;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setCumulativeTime(long cumulativeTime) {
		this.cumulativeTime = cumulativeTime;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}

	public long getAverageTime() {
		return this.cumulativeTime / this.count;
	}

	public String getOperation() {
		return operation;
	}

	public int getCount() {
		return count;
	}

	public int getFailed() {
		return failed;
	}

	public long getCumulativeTime() {
		return cumulativeTime;
	}

	public int getUniqueBindingsCount() {
		return uniqueBindingsCount != null ? uniqueBindingsCount : bindingsHashcodes.size();
	}

	public AggregatedOperationStats buildNext(OperationExecutionStats stats) {
		String newOperation = stats.getOperation();
		AggregatedOperationStats newStats;
		newStats = (AggregatedOperationStats) this.clone();
		if (newStats.operation != null) {
			if (!newStats.operation.equals(newOperation)) {
				throw new IllegalArgumentException(
						"Cannot add to aggregated stats: operations differ. Existing operation:\n"
								+ newStats.operation
								+ "\n, new operation:\n"
								+ newOperation);
			}
		} else {
			newStats.operation = newOperation;
		}
		newStats.bindingsHashcodes.add(stats.getBindingsHashCode());
		newStats.count += 1;
		if (stats.isFailed()) {
			newStats.failed += 1;
		}
		newStats.cumulativeTime += stats.getQueryDuration();
		return newStats;
	}
}
