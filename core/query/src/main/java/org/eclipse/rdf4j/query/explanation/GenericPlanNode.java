/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.explanation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.annotation.Experimental;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is an experimental feature. The interface may be changed, moved or potentially removed in a future release.
 *
 * The interface is used to implement query explanations (query plan)
 *
 * @since 3.2.0
 */
@Experimental
public class GenericPlanNode {

	public static final String UNKNOWN = "UNKNOWN";

	// static UUID as prefix together with a thread safe incrementing long ensures a unique identifier.
	private final static String uniqueIdPrefix = UUID.randomUUID().toString().replace("-", "");
	private final static AtomicLong uniqueIdSuffix = new AtomicLong();

	private final static String newLine = System.getProperty("line.separator");

	private final String id = "UUID_" + uniqueIdPrefix + uniqueIdSuffix.incrementAndGet();

	// The name of the node, eg. "Join" or "Join (HashJoinIteration)".
	private String type;

	// Retrieving the explanation timed out while the query was executed.
	private Boolean timedOut;

	// The cost estimate that the query planner calculated for this node. Value has no meaning outside of this
	// explanation and is only used to compare and order the nodes in the query plan.
	private Double costEstimate;

	// The number of results that this node was estimated to produce.
	private Double resultSizeEstimate;

	// The actual number of results that this node produced while the query was executed.
	private Long resultSizeActual;

	// The total time in milliseconds that this node-tree (all children and so on) used while the query was executed.
	// selfTimeActual is the amount of time that this node used by itself (eg. totalTimeActual - sum of
	// plans[0..n].totalTimeActual)
	private Double totalTimeActual;

	// true if this node introduces a new scope
	private Boolean newScope;

	// the name of the algorithm used as an annotation to the node type
	private String algorithm;

	// Child plans for this node
	private List<GenericPlanNode> plans = new ArrayList<>();

	public GenericPlanNode() {
	}

	public GenericPlanNode(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<GenericPlanNode> getPlans() {
		return plans.isEmpty() ? null : plans; // for simplified json
	}

	public void setPlans(List<GenericPlanNode> plans) {
		this.plans = plans;
	}

	public void addPlans(GenericPlanNode... children) {
		this.plans.addAll(Arrays.asList(children));
	}

	/**
	 * The cost estimate that the query planner calculated for this node. Value has no meaning outside of this
	 * explanation and is only used to compare and order the nodes in the query plan.
	 *
	 * @return a cost estimate as a double value
	 */
	public Double getCostEstimate() {
		return costEstimate;
	}

	public void setCostEstimate(Double costEstimate) {
		if (costEstimate >= 0) {
			this.costEstimate = costEstimate;
		}
	}

	/**
	 * The number of results that this node was estimated to produce.
	 *
	 * @return result size estimate
	 */
	public Double getResultSizeEstimate() {
		return resultSizeEstimate;
	}

	public void setResultSizeEstimate(Double resultSizeEstimate) {
		if (resultSizeEstimate >= 0) {
			this.resultSizeEstimate = resultSizeEstimate;
		}
	}

	/**
	 * The actual number of results that this node produced while the query was executed.
	 *
	 * @return number of results that this query produced
	 */
	public Long getResultSizeActual() {
		return resultSizeActual;
	}

	public void setResultSizeActual(Long resultSizeActual) {
		if (resultSizeActual >= 0) {
			this.resultSizeActual = resultSizeActual;
		}
	}

	/**
	 * The total time in milliseconds that this node-tree (all children and so on) used while the query was executed.
	 *
	 * @return time in milliseconds that was used to execute the query
	 */
	public Double getTotalTimeActual() {
		// Not all nodes have their own totalTimeActual, but it can easily be calculated by looking that the child plans
		// (recursively). We need this value to calculate the selfTimeActual.
		if (totalTimeActual == null) {
			double sum = plans.stream()
					.map(GenericPlanNode::getTotalTimeActual)
					.filter(Objects::nonNull)
					.mapToDouble(d -> d)
					.sum();

			if (sum > 0) {
				return sum;
			}
		}
		return totalTimeActual;
	}

	public void setTotalTimeActual(Double totalTimeActual) {
		if (totalTimeActual >= 0) {
			this.totalTimeActual = totalTimeActual;
		}
	}

	public void setTimedOut(Boolean timedOut) {
		this.timedOut = timedOut;
	}

	public Boolean getTimedOut() {
		return timedOut;
	}

	/**
	 * The time that this node used by itself (eg. totalTimeActual - sum of plans[0..n].totalTimeActual)
	 *
	 */
	public Double getSelfTimeActual() {

		if (totalTimeActual == null) {
			return null;
		}

		double childTime = plans
				.stream()
				.map(GenericPlanNode::getTotalTimeActual)
				.filter(Objects::nonNull)
				.mapToDouble(t -> t)
				.sum();

		return totalTimeActual - childTime;

	}

	/**
	 *
	 * @return true if this node introduces a new scope
	 */
	public Boolean isNewScope() {
		return newScope;
	}

	public void setNewScope(boolean newScope) {
		if (newScope) {
			this.newScope = true;
		} else {
			this.newScope = null;
		}
	}

	/**
	 * Join nodes can use various algorithms for joining data.
	 *
	 * @return the name of the algorithm.
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	private static final int prettyBoxDrawingType = 0;

	/**
	 * Human readable string. Do not attempt to parse this.
	 *
	 * @return an unparsable string
	 */
	@Override
	public String toString() {
		return getHumanReadable(0);
	}

	/**
	 *
	 * @param prettyBoxDrawingType for deciding if we should use single or double walled character for drawing the
	 *                             connectors between nodes in the query plan. Eg. ├ or ╠ and ─ o
	 * @return
	 */
	private String getHumanReadable(int prettyBoxDrawingType) {
		StringBuilder sb = new StringBuilder();

		if (timedOut != null && timedOut) {
			sb.append("Timed out while retrieving explanation! Explanation may be incomplete!").append(newLine);
			sb.append("You can change the timeout by setting .setMaxExecutionTime(...) on your query.")
					.append(newLine)
					.append(newLine);
		}

		sb.append(type);
		if (newScope != null && newScope) {
			sb.append(" (new scope)");
		}

		if (algorithm != null) {
			sb.append(" (").append(algorithm).append(")");
		}
		appendCostAnnotation(sb);
		sb.append(newLine);

		// we use box-drawing characters to "group" nodes in the plan visually when there are exactly two child plans
		// and
		// the child plans contain child plans
		if (plans.size() == 2 && plans.stream().anyMatch(p -> !p.plans.isEmpty())) {

			String start;
			String horizontal;
			String vertical;
			String end;

			if (prettyBoxDrawingType % 2 == 0) {
				start = "╠";
				horizontal = "══";
				vertical = "║";
				end = "╚";
			} else {
				start = "├";
				horizontal = "──";
				vertical = "│";
				end = "└";
			}

			String left = plans.get(0).getHumanReadable(prettyBoxDrawingType + 1);
			String right = plans.get(1).getHumanReadable(prettyBoxDrawingType + 1);
			{
				String[] split = left.split(newLine);
				sb.append(start).append(horizontal).append(split[0]).append(newLine);
				for (int i = 1; i < split.length; i++) {
					sb.append(vertical).append("  ").append(split[i]).append(newLine);
				}
			}

			{
				String[] split = right.split(newLine);
				sb.append(end).append(horizontal).append(split[0]).append(newLine);
				for (int i = 1; i < split.length; i++) {
					sb.append("   ").append(split[i]).append(newLine);
				}
			}

		} else {
			plans.forEach(
					child -> sb.append(Arrays.stream(child.getHumanReadable(prettyBoxDrawingType + 1).split(newLine))
							.map(c -> "   " + c)
							.reduce((a, b) -> a + newLine + b)
							.orElse("") + newLine));
		}

		return sb.toString();
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static private String toHumanReadableNumber(Double number) {
		String humanReadbleString;
		if (number == null) {
			humanReadbleString = UNKNOWN;
		} else if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = Math.round(number / 100_000) / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = Math.round(number / 100) / 10.0 + "K";
		} else if (number >= 0) {
			humanReadbleString = Math.round(number) + "";
		} else {
			humanReadbleString = UNKNOWN;
		}

		return humanReadbleString;
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static private String toHumanReadableNumber(Long number) {
		String humanReadbleString;
		if (number == null) {
			humanReadbleString = UNKNOWN;
		} else if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = number / 100_000 / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = number / 100 / 10.0 + "K";
		} else if (number >= 0) {
			humanReadbleString = number + "";
		} else {
			humanReadbleString = UNKNOWN;
		}

		return humanReadbleString;
	}

	/**
	 *
	 * @return Human readable time.
	 */
	static private String toHumanReadableTime(Double millis) {
		String humanReadbleString;

		if (millis == null) {
			humanReadbleString = UNKNOWN;
		} else if (millis > 1_000) {
			humanReadbleString = Math.round(millis / 100) / 10.0 + "s";
		} else if (millis >= 100) {
			humanReadbleString = Math.round(millis) + "ms";
		} else if (millis >= 10) {
			humanReadbleString = Math.round(millis * 10) / 10.0 + "ms";
		} else if (millis >= 1) {
			humanReadbleString = Math.round(millis * 100) / 100.0 + "ms";
		} else if (millis >= 0) {
			humanReadbleString = Math.round(millis * 1000) / 1000.0 + "ms";
		} else {
			humanReadbleString = UNKNOWN;
		}

		return humanReadbleString;
	}

	private void appendCostAnnotation(StringBuilder sb) {
		String costs = Stream.of(
				"costEstimate=" + toHumanReadableNumber(getCostEstimate()),
				"resultSizeEstimate=" + toHumanReadableNumber(getResultSizeEstimate()),
				"resultSizeActual=" + toHumanReadableNumber(getResultSizeActual()),
				"totalTimeActual=" + toHumanReadableTime(getTotalTimeActual()),
				"selfTimeActual=" + toHumanReadableTime(getSelfTimeActual()))
				.filter(s -> !s.endsWith(UNKNOWN)) // simple but hacky way of removing essentially null values
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		if (!costs.isEmpty()) {
			sb.append(" (").append(costs).append(")");
		}
	}

	public String toDot() {

		return toDotInternal(getMaxResultSizeActual(this), getMaxTotalTime(this), getMaxSelfTime(this));

	}

	private static double getMaxTotalTime(GenericPlanNode genericPlanNode) {
		return Math.max(genericPlanNode.getTotalTimeActual() != null ? genericPlanNode.getTotalTimeActual() : 0,
				genericPlanNode.plans.stream().mapToDouble(GenericPlanNode::getMaxTotalTime).max().orElse(0));
	}

	private static double getMaxSelfTime(GenericPlanNode genericPlanNode) {
		return Math.max(genericPlanNode.getSelfTimeActual() != null ? genericPlanNode.getSelfTimeActual() : 0,
				genericPlanNode.plans.stream().mapToDouble(GenericPlanNode::getMaxSelfTime).max().orElse(0));
	}

	private static double getMaxResultSizeActual(GenericPlanNode genericPlanNode) {
		return Math.max(genericPlanNode.getResultSizeActual() != null ? genericPlanNode.getResultSizeActual() : 0,
				genericPlanNode.plans.stream().mapToDouble(GenericPlanNode::getMaxResultSizeActual).max().orElse(0));
	}

	private String toDotInternal(double maxResultSizeActual, double maxTotalTime, double maxSelfTime) {
		StringBuilder sb = new StringBuilder();
		sb.append("   ");

		if (newScope != null && newScope) {
			sb.append("subgraph cluster_")
					.append(getID())
					.append(" {")
					.append(newLine)
					.append("   color=grey")
					.append(newLine);
		}

		String resultSizeActualColor = getProportionalRedColor(maxResultSizeActual, getResultSizeActual());
		String totalTimeColor = getProportionalRedColor(maxTotalTime, getTotalTimeActual());
		String selfTimeColor = getProportionalRedColor(maxSelfTime, getSelfTimeActual());

		sb
				.append(getID())
				.append(" [label=")
				.append("<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" >");

		sb.append(Stream.of(
				"<tr><td COLSPAN=\"2\" BGCOLOR=\"" + totalTimeColor + "\"><U>" + StringEscapeUtils.escapeHtml4(type)
						+ "</U></td></tr>",
				"<tr><td>Algorithm</td><td>" + (algorithm != null ? algorithm : UNKNOWN) + "</td></tr>",
				"<tr><td><B>New scope</B></td><td>" + (newScope != null && newScope ? "<B>true</B>" : UNKNOWN)
						+ "</td></tr>",
				"<tr><td>Cost estimate</td><td>" + toHumanReadableNumber(getCostEstimate()) + "</td></tr>",
				"<tr><td>Result size estimate</td><td>" + toHumanReadableNumber(getResultSizeEstimate()) + "</td></tr>",
				"<tr><td >Result size actual</td><td>" + toHumanReadableNumber(getResultSizeActual()) + "</td></tr>",
//			"<tr><td >Result size actual</td><td BGCOLOR=\"" + resultSizeActualColor + "\">" + toHumanReadableNumber(getResultSizeActual()) + "</td></tr>",
				"<tr><td >Total time actual</td><td BGCOLOR=\"" + totalTimeColor + "\">"
						+ toHumanReadableTime(getTotalTimeActual()) + "</td></tr>",
				"<tr><td >Self time actual</td><td BGCOLOR=\"" + selfTimeColor + "\">"
						+ toHumanReadableTime(getSelfTimeActual()) + "</td></tr>")
				.filter(s -> !s.contains(UNKNOWN)) // simple but hacky way of removing essentially null values
				.reduce((a, b) -> a + " " + b)
				.orElse(""));

		sb.append("</table>>").append(" shape=plaintext];").append(newLine);
		for (int i = 0; i < plans.size(); i++) {
			GenericPlanNode p = plans.get(i);
			String linkLabel = "index " + i;

			if (plans.size() == 2) {
				linkLabel = i == 0 ? "left" : "right";
			} else if (plans.size() == 1) {
				linkLabel = "";
			}

			sb.append("   ")
					.append(getID())
					.append(" -> ")
					.append(p.getID())
					.append(" [label=\"")
					.append(linkLabel)
					.append("\"]")
					.append(" ;")
					.append(newLine);
		}

		plans.forEach(p -> sb.append(p.toDotInternal(maxResultSizeActual, maxTotalTime, maxSelfTime)));

		if (newScope != null && newScope) {
			sb.append(newLine).append("}").append(newLine);
		}
		return sb.toString();
	}

	private String getProportionalRedColor(Double max, Double value) {
		String mainColor = "#FFFFFF";
		if (value != null) {
			double colorInt = Math.abs(256 / max * value - 256);
			String hexColor = String.format("%02X", (0xFFFFFF & ((int) Math.floor(colorInt))));

			mainColor = "#FF" + hexColor + hexColor;
		}
		return mainColor;
	}

	private String getProportionalRedColor(Double max, Long value) {
		if (value != null) {
			return getProportionalRedColor(max, value + 0.0);
		}
		return "#FFFFFF";
	}

	@JsonIgnore
	public String getID() {
		return id;
	}
}
