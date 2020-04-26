/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.explanation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.Experimental;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is an experimental feature. The interface may be changed, moved or potentially removed in a future release.
 *
 * The interface is used to implement query explanations (query plan)
 */
@Experimental
public class GenericPlanNode {

	private String type;

	private Double costEstimate;
	private Double resultSizeEstimate;
	private Long resultSizeActual;
	private Double totalTime;

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

	public Double getCostEstimate() {
		return costEstimate;
	}

	public void setCostEstimate(Double costEstimate) {
		if (costEstimate >= 0) {
			this.costEstimate = costEstimate;
		}
	}

	public Double getResultSizeEstimate() {
		return resultSizeEstimate;
	}

	public void setResultSizeEstimate(Double resultSizeEstimate) {
		if (resultSizeEstimate >= 0) {
			this.resultSizeEstimate = resultSizeEstimate;
		}
	}

	public Long getResultSizeActual() {
		return resultSizeActual;
	}

	public void setResultSizeActual(Long resultSizeActual) {
		if (resultSizeActual >= 0) {
			this.resultSizeActual = resultSizeActual;
		}
	}

	public Double getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(Double totalTime) {
		if (totalTime >= 0) {
			this.totalTime = totalTime;
		}
	}

	public Double getSelfTime() {

		if (totalTime == null) {
			return null;
		}

		double childTime = plans
				.stream()
				.map(GenericPlanNode::getTotalTime)
				.filter(Objects::nonNull)
				.mapToDouble(t -> t)
				.sum();

		return totalTime - childTime;

	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		String newLine = System.getProperty("line.separator");

		sb.append(type);
		appendCostAnnotation(sb);
		sb.append(newLine);
		plans.forEach(child -> sb.append(Arrays.stream(
				child.toString()
						.split(newLine))
				.map(c -> "   " + c)
				.reduce((a, b) -> a + newLine + b)
				.orElse("") + newLine));

		return sb.toString();
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static String toHumanReadableNumber(Double number) {
		String humanReadbleString;
		if (number == null) {
			humanReadbleString = "UNKNOWN";
		} else if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = Math.round(number / 100_000) / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = Math.round(number / 100) / 10.0 + "K";
		} else if (number >= 0) {
			humanReadbleString = Math.round(number) + "";
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static String toHumanReadableNumber(Long number) {
		String humanReadbleString;
		if (number == null) {
			humanReadbleString = "UNKNOWN";
		} else if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = number / 100_000 / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = number / 100 / 10.0 + "K";
		} else if (number >= 0) {
			humanReadbleString = number + "";
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}

	/**
	 *
	 * @return Human readable time.
	 */
	static String toHumanReadableTime(Double millis) {
		String humanReadbleString;

		if (millis == null) {
			humanReadbleString = "UNKNOWN";
		} else if (millis > 1_000) {
			humanReadbleString = millis / 100 / 10.0 + "s";
		} else if (millis >= 0) {
			humanReadbleString = Math.round(millis * 1000) / 1000.0 + "ms";
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}

	protected void appendCostAnnotation(StringBuilder sb) {
		String costs = Stream.of(
				"costEstimate=" + toHumanReadableNumber(getCostEstimate()),
				"resultSizeEstimate=" + toHumanReadableNumber(getResultSizeEstimate()),
				"resultSizeActual=" + toHumanReadableNumber(getResultSizeActual()),
				"totalTime=" + toHumanReadableTime(getTotalTime()),
				"selfTime=" + toHumanReadableTime(getSelfTime()))
				.filter(s -> !s.endsWith("UNKNOWN"))
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		if (!costs.isEmpty()) {
			sb.append(" (").append(costs).append(")");
		}
	}

}
