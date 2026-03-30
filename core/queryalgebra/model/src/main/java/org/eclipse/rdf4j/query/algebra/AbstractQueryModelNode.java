/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.query.algebra.helpers.QueryModelTreePrinter;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base implementation of {@link QueryModelNode}.
 */
public abstract class AbstractQueryModelNode implements QueryModelNode, VariableScopeChange {

	private static final double CARDINALITY_NOT_SET = Double.MIN_VALUE;

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final long serialVersionUID = 3006199552086476178L;

	@JsonIgnore
	private QueryModelNode parent;

	private boolean isVariableScopeChange;

	private double resultSizeEstimate = -1;
	private long resultSizeActual = -1;
	private double costEstimate = -1;
	private long totalTimeNanosActual = -1;
	private long hasNextCallCountActual = -1;
	private long hasNextTrueCountActual = -1;
	private long hasNextTimeNanosActual = -1;
	private long nextCallCountActual = -1;
	private long nextTimeNanosActual = -1;
	private long joinRightIteratorsCreatedActual = -1;
	private long joinLeftBindingsConsumedActual = -1;
	private long joinRightBindingsConsumedActual = -1;
	private long sourceRowsScannedActual = -1;
	private long sourceRowsMatchedActual = -1;
	private long sourceRowsFilteredActual = -1;
	private boolean runtimeTelemetryEnabled;
	private Map<String, Long> longMetricsActual = Collections.emptyMap();
	private Map<String, Double> doubleMetricsActual = Collections.emptyMap();
	private Map<String, String> stringMetricsActual = Collections.emptyMap();

	private double cardinality = CARDINALITY_NOT_SET;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public QueryModelNode getParentNode() {
		return parent;
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		this.parent = parent;
	}

	@Override
	public boolean isVariableScopeChange() {
		return isVariableScopeChange;
	}

	@Override
	public void setVariableScopeChange(boolean isVariableScopeChange) {
		this.isVariableScopeChange = isVariableScopeChange;
	}

	/**
	 * Default implementation of {@link QueryModelNode#replaceWith(QueryModelNode)} that throws an
	 * {@link IllegalArgumentException} indicating that <var>current</var> is not a child node of this node.
	 */
	@Override
	public void replaceWith(QueryModelNode replacement) {
		if (parent == null) {
			throw new IllegalStateException("Node has no parent");
		}

		parent.replaceChildNode(this, replacement);
	}

	/**
	 * Default implementation of {@link QueryModelNode#getSignature()} that prints the name of the node's class.
	 */
	@Override
	public String getSignature() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String toString() {
		QueryModelTreePrinter treePrinter = new QueryModelTreePrinter();
		this.visit(treePrinter);
		return treePrinter.getTreeString();
	}

	@Override
	public AbstractQueryModelNode clone() {
		try {
			AbstractQueryModelNode clone = (AbstractQueryModelNode) super.clone();
			clone.setVariableScopeChange(this.isVariableScopeChange());
			clone.cardinality = CARDINALITY_NOT_SET;
			clone.parent = null;
			clone.longMetricsActual = longMetricsActual.isEmpty() ? Collections.emptyMap()
					: new HashMap<>(longMetricsActual);
			clone.doubleMetricsActual = doubleMetricsActual.isEmpty() ? Collections.emptyMap()
					: new HashMap<>(doubleMetricsActual);
			clone.stringMetricsActual = stringMetricsActual.isEmpty() ? Collections.emptyMap()
					: new HashMap<>(stringMetricsActual);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Query model nodes are required to be cloneable", e);
		}
	}

	protected <T extends QueryModelNode> boolean replaceNodeInList(List<T> list, QueryModelNode current,
			QueryModelNode replacement) {
		ListIterator<T> iter = list.listIterator();
		while (iter.hasNext()) {
			if (iter.next() == current) {
				iter.set((T) replacement);
				replacement.setParentNode(this);
				return true;
			}
		}

		return false;
	}

	protected boolean nullEquals(Object o1, Object o2) {
		return Objects.equals(o1, o2);
	}

	@Override
	public double getResultSizeEstimate() {
		return resultSizeEstimate;
	}

	@Override
	public void setResultSizeEstimate(double resultSizeEstimate) {
		this.resultSizeEstimate = resultSizeEstimate;
	}

	@Override
	public long getResultSizeActual() {
		return resultSizeActual;
	}

	@Override
	public void setResultSizeActual(long resultSizeActual) {
		this.resultSizeActual = resultSizeActual;
	}

	@Override
	public double getCostEstimate() {
		return costEstimate;
	}

	@Override
	public void setCostEstimate(double costEstimate) {
		this.costEstimate = costEstimate;
	}

	@Override
	public long getTotalTimeNanosActual() {
		return totalTimeNanosActual;
	}

	@Override
	public void setTotalTimeNanosActual(long totalTimeNanosActual) {
		this.totalTimeNanosActual = totalTimeNanosActual;
	}

	@Override
	public long getHasNextCallCountActual() {
		return hasNextCallCountActual;
	}

	@Override
	public void setHasNextCallCountActual(long hasNextCallCountActual) {
		this.hasNextCallCountActual = hasNextCallCountActual;
	}

	@Override
	public long getHasNextTrueCountActual() {
		return hasNextTrueCountActual;
	}

	@Override
	public void setHasNextTrueCountActual(long hasNextTrueCountActual) {
		this.hasNextTrueCountActual = hasNextTrueCountActual;
	}

	@Override
	public long getHasNextTimeNanosActual() {
		return hasNextTimeNanosActual;
	}

	@Override
	public void setHasNextTimeNanosActual(long hasNextTimeNanosActual) {
		this.hasNextTimeNanosActual = hasNextTimeNanosActual;
	}

	@Override
	public long getNextCallCountActual() {
		return nextCallCountActual;
	}

	@Override
	public void setNextCallCountActual(long nextCallCountActual) {
		this.nextCallCountActual = nextCallCountActual;
	}

	@Override
	public long getNextTimeNanosActual() {
		return nextTimeNanosActual;
	}

	@Override
	public void setNextTimeNanosActual(long nextTimeNanosActual) {
		this.nextTimeNanosActual = nextTimeNanosActual;
	}

	@Override
	public long getJoinRightIteratorsCreatedActual() {
		return joinRightIteratorsCreatedActual;
	}

	@Override
	public void setJoinRightIteratorsCreatedActual(long joinRightIteratorsCreatedActual) {
		this.joinRightIteratorsCreatedActual = joinRightIteratorsCreatedActual;
	}

	@Override
	public long getJoinLeftBindingsConsumedActual() {
		return joinLeftBindingsConsumedActual;
	}

	@Override
	public void setJoinLeftBindingsConsumedActual(long joinLeftBindingsConsumedActual) {
		this.joinLeftBindingsConsumedActual = joinLeftBindingsConsumedActual;
	}

	@Override
	public long getJoinRightBindingsConsumedActual() {
		return joinRightBindingsConsumedActual;
	}

	@Override
	public void setJoinRightBindingsConsumedActual(long joinRightBindingsConsumedActual) {
		this.joinRightBindingsConsumedActual = joinRightBindingsConsumedActual;
	}

	@Override
	public long getSourceRowsScannedActual() {
		return sourceRowsScannedActual;
	}

	@Override
	public void setSourceRowsScannedActual(long sourceRowsScannedActual) {
		this.sourceRowsScannedActual = sourceRowsScannedActual;
	}

	@Override
	public long getSourceRowsMatchedActual() {
		return sourceRowsMatchedActual;
	}

	@Override
	public void setSourceRowsMatchedActual(long sourceRowsMatchedActual) {
		this.sourceRowsMatchedActual = sourceRowsMatchedActual;
	}

	@Override
	public long getSourceRowsFilteredActual() {
		return sourceRowsFilteredActual;
	}

	@Override
	public void setSourceRowsFilteredActual(long sourceRowsFilteredActual) {
		this.sourceRowsFilteredActual = sourceRowsFilteredActual;
	}

	@Override
	public Map<String, Long> getLongMetricsActual() {
		return longMetricsActual;
	}

	@Override
	public long getLongMetricActual(String metricName) {
		return longMetricsActual.getOrDefault(metricName, -1L);
	}

	@Override
	public void setLongMetricActual(String metricName, long metricValue) {
		if (metricName == null || !runtimeTelemetryEnabled) {
			return;
		}
		if (longMetricsActual.isEmpty()) {
			longMetricsActual = new HashMap<>();
		}
		longMetricsActual.put(metricName, metricValue);
	}

	@Override
	public Map<String, Double> getDoubleMetricsActual() {
		return doubleMetricsActual;
	}

	@Override
	public double getDoubleMetricActual(String metricName) {
		return doubleMetricsActual.getOrDefault(metricName, -1D);
	}

	@Override
	public void setDoubleMetricActual(String metricName, double metricValue) {
		if (metricName == null || !runtimeTelemetryEnabled) {
			return;
		}
		if (doubleMetricsActual.isEmpty()) {
			doubleMetricsActual = new HashMap<>();
		}
		doubleMetricsActual.put(metricName, metricValue);
	}

	@Override
	public Map<String, String> getStringMetricsActual() {
		return stringMetricsActual;
	}

	@Override
	public String getStringMetricActual(String metricName) {
		return stringMetricsActual.get(metricName);
	}

	@Override
	public void setStringMetricActual(String metricName, String metricValue) {
		if (metricName == null || !runtimeTelemetryEnabled) {
			return;
		}
		if (stringMetricsActual.isEmpty()) {
			stringMetricsActual = new HashMap<>();
		}
		stringMetricsActual.put(metricName, metricValue);
	}

	@Override
	public boolean isRuntimeTelemetryEnabled() {
		return runtimeTelemetryEnabled;
	}

	@Override
	public void setRuntimeTelemetryEnabled(boolean runtimeTelemetryEnabled) {
		this.runtimeTelemetryEnabled = runtimeTelemetryEnabled;
	}

	/**
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static String toHumanReadableNumber(double number) {
		String humanReadbleString;
		if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = Math.round(number / 100_000) / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = Math.round(number / 100) / 10.0 + "K";
		} else if (number < 10 && number > 0) {
			humanReadbleString = String.format("%.2f", number);
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}

	@Experimental
	public double getCardinality() {
		assert cardinality != CARDINALITY_NOT_SET;
		return cardinality;
	}

	@Experimental
	public void setCardinality(double cardinality) {
		this.cardinality = cardinality;
	}

	@Experimental
	public void resetCardinality() {
		this.cardinality = CARDINALITY_NOT_SET;
	}

	@Experimental
	public boolean isCardinalitySet() {
		return shouldCacheCardinality() && cardinality != CARDINALITY_NOT_SET;
	}

	@Experimental
	protected boolean shouldCacheCardinality() {
		return false;
	}

}
