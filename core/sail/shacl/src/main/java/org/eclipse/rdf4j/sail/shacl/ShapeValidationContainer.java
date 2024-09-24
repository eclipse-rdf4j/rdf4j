/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SingleCloseablePlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.results.lazy.ValidationResultIterator;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;

class ShapeValidationContainer {
	private final Shape shape;
	private final boolean logValidationViolations;
	private final PlanNode planNode;
	private final ValidationExecutionLogger validationExecutionLogger;
	private final long effectiveValidationResultsLimitPerConstraint;
	private final boolean performanceLogging;
	private final Logger logger;

	public ShapeValidationContainer(Shape shape, Supplier<PlanNode> planNodeSupplier, boolean logValidationExecution,
			boolean logValidationViolations, long effectiveValidationResultsLimitPerConstraint,
			boolean performanceLogging, boolean logValidationPlans, Logger logger, ConnectionsGroup connectionsGroup) {
		this.shape = shape;
		this.logValidationViolations = logValidationViolations;
		this.effectiveValidationResultsLimitPerConstraint = effectiveValidationResultsLimitPerConstraint;
		this.performanceLogging = performanceLogging;
		this.logger = logger;
		try {
			PlanNode planNode = planNodeSupplier.get();

			if (logValidationPlans) {

				StringBuilder planAsGraphvizDot = new StringBuilder();

				planAsGraphvizDot.append(
						"rank1 [style=invisible];\n" +
								"rank2 [style=invisible];\n" +
								"\n" +
								"rank1 -> rank2 [color=white];\n");

				planAsGraphvizDot.append("{\n")
						.append("\trank = same;\n")
						.append("\trank2 -> ")
						.append(System.identityHashCode(connectionsGroup.getBaseConnection()))
						.append(" -> ")
						.append(System.identityHashCode(connectionsGroup.getAddedStatements()))
						.append(" -> ")
						.append(System.identityHashCode(connectionsGroup.getRemovedStatements()))
						.append(" [ style=invis ];\n")
						.append("\trankdir = LR;\n")
						.append("}\n");

				planAsGraphvizDot.append(System.identityHashCode(connectionsGroup.getBaseConnection()))
						.append(" [label=\"")
						.append("BaseConnection")
						.append("\" fillcolor=\"#CACADB\", style=filled];")
						.append("\n");

				planAsGraphvizDot.append(System.identityHashCode(connectionsGroup.getAddedStatements()))
						.append(" [label=\"")
						.append("AddedStatements")
						.append("\" fillcolor=\"#CEDBCA\", style=filled];")
						.append("\n");

				planAsGraphvizDot.append(System.identityHashCode(connectionsGroup.getRemovedStatements()))
						.append(" [label=\"")
						.append("RemovedStatements")
						.append("\" fillcolor=\"#DBCFC9r\", style=filled];")
						.append("\n");

				planNode.getPlanAsGraphvizDot(planAsGraphvizDot);

				String[] split = planAsGraphvizDot.toString().split("\n");
				planAsGraphvizDot = new StringBuilder();
				Arrays.stream(split).map(s -> "\t" + s + "\n").forEach(planAsGraphvizDot::append);

				logger.info("Plan as Graphviz dot:\ndigraph G {\n{}}", planAsGraphvizDot);
			}

			this.validationExecutionLogger = ValidationExecutionLogger
					.getInstance(logValidationExecution);
			if (!(planNode.isGuaranteedEmpty())) {
				assert planNode instanceof SingleCloseablePlanNode;
				planNode.receiveLogger(validationExecutionLogger);
				this.planNode = planNode;
			} else {
				this.planNode = planNode;
			}
		} catch (Throwable e) {
			logger.warn("Error processing SHACL Shape {}", shape.getId(), e);
			logger.warn("Error processing SHACL Shape\n{}", shape, e);
			if (e instanceof Error) {
				throw e;
			}
			throw new SailException("Error processing SHACL Shape " + shape.getId() + "\n" + shape, e);
		}

	}

	public Shape getShape() {
		return shape;
	}

	public boolean hasPlanNode() {
		return !(planNode.isGuaranteedEmpty());
	}

	public ValidationResultIterator performValidation() {
		long before = getTimeStamp();
		handlePreLogging();

		ValidationResultIterator validationResults = null;

		try (CloseableIteration<? extends ValidationTuple> iterator = planNode.iterator()) {
			validationResults = new ValidationResultIterator(iterator, effectiveValidationResultsLimitPerConstraint);
			return validationResults;
		} catch (Throwable e) {
			logger.warn("Internal error while trying to validate SHACL Shape {}", shape.getId(), e);
			logger.warn("Internal error while trying to validate SHACL Shape\n{}", shape, e);
			if (e instanceof Error) {
				throw e;
			}
			throw new SailException(
					"Internal error while trying to validate SHACL Shape " + shape.getId() + "\n" + shape, e);
		} finally {
			handlePostLogging(before, validationResults);
		}
	}

	private long getTimeStamp() {
		if (performanceLogging) {
			return System.currentTimeMillis();
		}
		return 0;
	}

	private void handlePreLogging() {
		if (validationExecutionLogger.isEnabled()) {
			logger.info("Start execution of plan:\n{}\n", getShape().toString());
		}
	}

	private void handlePostLogging(long before, ValidationResultIterator validationResults) {
		if (validationExecutionLogger.isEnabled()) {
			validationExecutionLogger.flush();
		}

		if (validationResults != null) {
			if (performanceLogging) {
				long after = System.currentTimeMillis();
				logger.info("Execution of plan took {} ms for:\n{}\n",
						(after - before),
						getShape().toString());
			}

			if (validationExecutionLogger.isEnabled()) {
				logger.info("Finished execution of plan:\n{}\n",
						getShape().toString());
			}

			if (logValidationViolations) {
				if (!validationResults.conforms()) {
					List<ValidationTuple> tuples = validationResults.getTuples();

					logger.info(
							"SHACL not valid. The following experimental debug results were produced:\n\t\t{}\n\n{}\n",
							tuples.stream()
									.map(ValidationTuple::toString)
									.collect(Collectors.joining("\n\t\t")),
							getShape().toString()

					);
				}
			}

		}

	}

}
