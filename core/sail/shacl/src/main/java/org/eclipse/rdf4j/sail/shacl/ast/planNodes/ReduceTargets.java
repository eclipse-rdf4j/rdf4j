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

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Takes a parentToReduce and filters away any tuples that have an active target that exists in reductionSource
 */
public class ReduceTargets implements PlanNode {

	private final PlanNode parentToReduce;
	private final PlanNode reductionSource;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ReduceTargets(PlanNode parentToReduce, PlanNode reductionSource) {
		this.parentToReduce = PlanNodeHelper.handleSorting(this, parentToReduce);
		this.reductionSource = reductionSource;
	}

	@Override

	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parentToReduce
					.iterator();

			final Set<Value> reductionSourceSet = new HashSet<>();

			{
				try (CloseableIteration<? extends ValidationTuple, SailException> iterator = reductionSource
						.iterator()) {
					while (iterator.hasNext()) {
						reductionSourceSet.add(iterator.next().getActiveTarget());
					}
				}
			}

			ValidationTuple next;

			void calculateNext() {

				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();
					if (!reductionSourceSet.contains(temp.getActiveTarget())) {
						next = temp;
					}

				}

			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();

				return next != null;
			}

			@Override
			public void localClose() throws SailException {
				parentIterator.close();
			}

		};
	}

	@Override
	public int depth() {
		return parentToReduce.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId())
				.append(" [label=\"")
				.append(StringEscapeUtils.escapeJava(this.toString()))
				.append("\"];")
				.append("\n");
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parentToReduce.receiveLogger(validationExecutionLogger);
		reductionSource.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return parentToReduce.producesSorted();
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public String toString() {
		return "ReduceTargets{" +
				"parentToReduce=" + parentToReduce +
				", reductionSource=" + reductionSource +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ReduceTargets that = (ReduceTargets) o;

		if (!parentToReduce.equals(that.parentToReduce)) {
			return false;
		}
		return reductionSource.equals(that.reductionSource);
	}

	@Override
	public int hashCode() {
		return 31 * parentToReduce.hashCode() + reductionSource.hashCode();
	}
}
