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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * Main interface for all query model nodes.
 */
public interface QueryModelNode extends Cloneable, Serializable {

	/**
	 * Visits this node. The node reports itself to the visitor with the proper runtime type.
	 */
	<X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X;

	/**
	 * Visits the children of this node. The node calls {@link #visit(QueryModelVisitor)} on all of its child nodes.
	 */
	<X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X;

	/**
	 * Gets the node's parent.
	 *
	 * @return The parent node, if any.
	 */
	QueryModelNode getParentNode();

	/**
	 * Sets the node's parent.
	 *
	 * @param parent The parent node for this node.
	 */
	void setParentNode(QueryModelNode parent);

	/**
	 * Replaces one of the child nodes with a new node.
	 *
	 * @param current     The current child node.
	 * @param replacement The new child node.
	 * @throws IllegalArgumentException If <var>current</var> is not one of node's children.
	 * @throws ClassCastException       If <var>replacement</var> is of an incompatible type.
	 */
	void replaceChildNode(QueryModelNode current, QueryModelNode replacement);

	/**
	 * Substitutes this node with a new node in the query model tree.
	 *
	 * @param replacement The new node.
	 * @throws IllegalStateException If this node does not have a parent node.
	 * @throws ClassCastException    If <var>replacement</var> is of an incompatible type.
	 */
	void replaceWith(QueryModelNode replacement);

	/**
	 * Returns <var>true</var> if this query model node and its children are recursively equal to <var>o</var> and its
	 * children.
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Returns an indented print of the node tree, starting from this node.
	 */
	@Override
	String toString();

	/**
	 * Returns the signature of this query model node. Signatures normally include the node's name and any parameters,
	 * but not parent or child nodes. This method is used by {@link #toString()}.
	 *
	 * @return The node's signature, e.g. <var>SLICE (offset=10, limit=10)</var>.
	 */
	String getSignature();

	/**
	 * Returns a (deep) clone of this query model node. This method recursively clones the entire node tree, starting
	 * from this nodes.
	 *
	 * @return A deep clone of this query model node.
	 */
	QueryModelNode clone();

	/**
	 * Returns the number of tuples that this QueryNode predicts will be outputted. For a StatementPattern this would be
	 * the estimated cardinality provided by the EvaluationStatistics. For a Join the would be the resulting number of
	 * joined tuples.
	 *
	 * @return rows
	 */
	@Experimental
	default double getResultSizeEstimate() {
		return -1;
	}

	@Experimental
	default void setResultSizeEstimate(double rows) {
		// no-op for backwards compatibility
	}

	@Experimental
	default long getResultSizeActual() {
		return -1;
	}

	@Experimental
	default void setResultSizeActual(long resultSizeActual) {
		// no-op for backwards compatibility
	}

	@Experimental
	default double getCostEstimate() {
		return -1;
	}

	@Experimental
	default void setCostEstimate(double costEstimate) {
		// no-op for backwards compatibility
	}

	@Experimental
	default long getTotalTimeNanosActual() {
		return -1;
	}

	@Experimental
	default void setTotalTimeNanosActual(long totalTime) {
		// no-op
	}

	@Experimental
	default long getHasNextCallCountActual() {
		return -1;
	}

	@Experimental
	default void setHasNextCallCountActual(long hasNextCallCountActual) {
		// no-op
	}

	@Experimental
	default long getHasNextTrueCountActual() {
		return -1;
	}

	@Experimental
	default void setHasNextTrueCountActual(long hasNextTrueCountActual) {
		// no-op
	}

	@Experimental
	default long getHasNextTimeNanosActual() {
		return -1;
	}

	@Experimental
	default void setHasNextTimeNanosActual(long hasNextTimeNanosActual) {
		// no-op
	}

	@Experimental
	default long getNextCallCountActual() {
		return -1;
	}

	@Experimental
	default void setNextCallCountActual(long nextCallCountActual) {
		// no-op
	}

	@Experimental
	default long getNextTimeNanosActual() {
		return -1;
	}

	@Experimental
	default void setNextTimeNanosActual(long nextTimeNanosActual) {
		// no-op
	}

	@Experimental
	default long getJoinRightIteratorsCreatedActual() {
		return -1;
	}

	@Experimental
	default void setJoinRightIteratorsCreatedActual(long joinRightIteratorsCreatedActual) {
		// no-op
	}

	@Experimental
	default long getJoinLeftBindingsConsumedActual() {
		return -1;
	}

	@Experimental
	default void setJoinLeftBindingsConsumedActual(long joinLeftBindingsConsumedActual) {
		// no-op
	}

	@Experimental
	default long getJoinRightBindingsConsumedActual() {
		return -1;
	}

	@Experimental
	default void setJoinRightBindingsConsumedActual(long joinRightBindingsConsumedActual) {
		// no-op
	}

	@Experimental
	default long getSourceRowsScannedActual() {
		return -1;
	}

	@Experimental
	default void setSourceRowsScannedActual(long sourceRowsScannedActual) {
		// no-op
	}

	@Experimental
	default long getSourceRowsMatchedActual() {
		return -1;
	}

	@Experimental
	default void setSourceRowsMatchedActual(long sourceRowsMatchedActual) {
		// no-op
	}

	@Experimental
	default long getSourceRowsFilteredActual() {
		return -1;
	}

	@Experimental
	default void setSourceRowsFilteredActual(long sourceRowsFilteredActual) {
		// no-op
	}

	@Experimental
	default Map<String, Long> getLongMetricsActual() {
		return Collections.emptyMap();
	}

	@Experimental
	default long getLongMetricActual(String metricName) {
		return -1;
	}

	@Experimental
	default void setLongMetricActual(String metricName, long metricValue) {
		// no-op
	}

	@Experimental
	default Map<String, Double> getDoubleMetricsActual() {
		return Collections.emptyMap();
	}

	@Experimental
	default double getDoubleMetricActual(String metricName) {
		return -1;
	}

	@Experimental
	default void setDoubleMetricActual(String metricName, double metricValue) {
		// no-op
	}

	@Experimental
	default Map<String, String> getStringMetricsActual() {
		return Collections.emptyMap();
	}

	@Experimental
	default String getStringMetricActual(String metricName) {
		return null;
	}

	@Experimental
	default void setStringMetricActual(String metricName, String metricValue) {
		// no-op
	}

	@Experimental
	default boolean isRuntimeTelemetryEnabled() {
		return false;
	}

	@Experimental
	default void setRuntimeTelemetryEnabled(boolean runtimeTelemetryEnabled) {
		// no-op
	}

}
