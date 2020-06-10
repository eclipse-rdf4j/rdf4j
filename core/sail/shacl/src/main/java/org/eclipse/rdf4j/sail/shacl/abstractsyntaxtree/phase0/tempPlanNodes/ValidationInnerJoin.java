/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 *         <p>
 *         This inner join algorithm assumes the left iterator is unique for tuple[0], eg. no two tuples have the same
 *         value at index 0. The right iterator is allowed to contain duplicates.
 */
public class ValidationInnerJoin implements TupleValidationPlanNode {

	static private final Logger logger = LoggerFactory.getLogger(ValidationInnerJoin.class);
	private final boolean printed = false;

	private final TupleValidationPlanNode left;
	private final TupleValidationPlanNode right;
	private CloseableIteration<ValidationTuple, SailException> iterator;
	private ValidationExecutionLogger validationExecutionLogger;

	public ValidationInnerJoin(TupleValidationPlanNode left, TupleValidationPlanNode right) {
		this.left = left;
		this.right = right;
	}

	public List<TupleValidationPlanNode> parent() {
		return Arrays.asList(left, right);
	}

	@Override
	public CloseableIteration<ValidationTuple, SailException> iterator() {
		return internalIterator();
	}

	public CloseableIteration<ValidationTuple, SailException> internalIterator() {

		ValidationInnerJoin that = this;
		return new CloseableIteration<ValidationTuple, SailException>() {

			final CloseableIteration<ValidationTuple, SailException> leftIterator = left.iterator();
			final CloseableIteration<ValidationTuple, SailException> rightIterator = right.iterator();

			ValidationTuple next;
			ValidationTuple nextLeft;
			ValidationTuple nextRight;
			ValidationTuple joinedLeft;

			void calculateNext() {
				if (next != null) {
					return;
				}

				ValidationTuple prevLeft = nextLeft;
				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}

				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}

				if (nextRight == null && prevLeft == null && nextLeft != null) {

					return;
				}

				if (nextLeft == null) {

					return;
				}

				while (next == null) {
					if (nextRight != null) {

						if (nextLeft.sameTargetAs(nextRight)) {
							next = ValidationTupleHelper.join(nextLeft, nextRight);
							joinedLeft = nextLeft;
							nextRight = null;
						} else {

							int compareTo = nextLeft.compareTarget(nextRight);

							if (compareTo < 0) {

								if (leftIterator.hasNext()) {
									nextLeft = leftIterator.next();
								} else {
									nextLeft = null;
									break;
								}
							} else {

								if (rightIterator.hasNext()) {
									nextRight = rightIterator.next();
								} else {
									nextRight = null;
									break;
								}
							}

						}
					} else {

						return;
					}
				}

			}

			@Override
			public void close() throws SailException {
				leftIterator.close();
				rightIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			public ValidationTuple next() throws SailException {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return Math.max(left.depth(), right.depth());
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "InnerJoin";
	}

	private String leadingSpace() {
		return StringUtils.leftPad("", depth(), "    ");
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;

		TupleValidationPlanNode[] planNodes = { left, right };

		for (TupleValidationPlanNode planNode : planNodes) {
			if (planNode != null) {
				planNode.receiveLogger(validationExecutionLogger);
			}
		}

	}
}
