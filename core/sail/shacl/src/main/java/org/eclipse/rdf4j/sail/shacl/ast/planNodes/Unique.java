/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.CloseablePeakableIteration;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class Unique implements PlanNode {
	private final Logger logger = LoggerFactory.getLogger(Unique.class);
	private final boolean compress;
	private StackTraceElement[] stackTrace;

	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public Unique(PlanNode parent, boolean compress) {
		this.stackTrace = Thread.currentThread().getStackTrace();
		parent = PlanNodeHelper.handleSorting(this, parent);

		this.parent = parent;
		this.compress = compress;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseablePeakableIteration<? extends ValidationTuple, SailException> parentIterator = new CloseablePeakableIteration<>(
					parent.iterator());

			Set<ValidationTupleValueAndActiveTarget> targetAndValueDedupeSet;

			boolean propertyShapeWithValue;

			ValidationTuple next;
			ValidationTuple previous;

			private void calculateNext() {
				if (next != null) {
					return;
				}

//				if(parent instanceof Unique)

				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();

					if (temp.toString()
							.contains("chain=[http://example.com/ns#validPerson1, http://example.com/ns#p1]")) {
						System.out.println();
					}
					assert !propertyShapeWithValue
							|| temp.getScope() == ConstraintComponent.Scope.propertyShape && temp.hasValue();

					if (temp.getScope() == ConstraintComponent.Scope.propertyShape && temp.hasValue()) {
						propertyShapeWithValue = true;
					}

					if (compress) {
						Set<ValidationTuple> tuples = new HashSet<>();
						tuples.add(temp);

						if (propertyShapeWithValue) {
							while (parentIterator.hasNext()
									&& parentIterator.peek().getValue().equals(temp.getValue())) {
								tuples.add(parentIterator.next());
							}
						} else {
							while (parentIterator.hasNext() && parentIterator.peek().sameTargetAs(temp)) {
								tuples.add(parentIterator.next());
							}
						}

						if (tuples.size() == 1) {
							next = temp;
						} else {
							next = new ValidationTuple(temp, tuples);
						}

					} else if (previous == null) {
						next = temp;
					} else {
						if (propertyShapeWithValue) {
							if (targetAndValueDedupeSet == null || !previous.sameTargetAs(temp)) {
								targetAndValueDedupeSet = new HashSet<>();
								if (previous.sameTargetAs(temp)) {
									targetAndValueDedupeSet.add(new ValidationTupleValueAndActiveTarget(previous));
								}
							}

							if (!targetAndValueDedupeSet.contains(new ValidationTupleValueAndActiveTarget(temp))) {
								next = temp;
								targetAndValueDedupeSet.add(new ValidationTupleValueAndActiveTarget(next));
							}

						} else {
							if (!(previous.sameTargetAs(temp))) {
								next = temp;
							}
						}

					}

					if (next != null) {
						previous = next;
					} else {
						if (GlobalValidationExecutionLogging.loggingEnabled) {
							validationExecutionLogger.log(depth(),
									Unique.this.getClass().getSimpleName() + ":IgnoredNotUnique ", temp, Unique.this,
									getId(), stackTrace[2].toString());
						}
					}

				}

			}

			@Override
			public void close() throws SailException {
				targetAndValueDedupeSet = null;
				parentIterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			ValidationTuple loggingNext() throws SailException {
				calculateNext();
				if (previous != null && next.compareActiveTarget(previous) < 0) {
					throw new AssertionError();
				}
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
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Unique unique = (Unique) o;
		return parent.equals(unique.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, Unique.class);
	}

	@Override
	public String toString() {
		return "Unique{" +
				"compress=" + compress +
				", parent=" + parent +
				'}';
	}

	static class ValidationTupleValueAndActiveTarget {

		private final ValidationTuple validationTuple;

		public ValidationTupleValueAndActiveTarget(ValidationTuple validationTuple) {
			this.validationTuple = validationTuple;
		}

		public ValidationTuple getValidationTuple() {
			return validationTuple;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ValidationTupleValueAndActiveTarget validationTupleValueAndActiveTarget = (ValidationTupleValueAndActiveTarget) o;

			if (validationTuple.hasValue() || validationTupleValueAndActiveTarget.validationTuple.hasValue()) {
				assert validationTuple.hasValue() && validationTupleValueAndActiveTarget.validationTuple.hasValue();
				return validationTuple.getValue().equals(validationTupleValueAndActiveTarget.validationTuple.getValue())
						&& validationTuple.getActiveTarget()
								.equals(validationTupleValueAndActiveTarget.validationTuple.getActiveTarget());
			}

			return validationTuple.getActiveTarget()
					.equals(validationTupleValueAndActiveTarget.validationTuple.getActiveTarget());
		}

		@Override
		public int hashCode() {
			return Objects.hash(validationTuple.getActiveTarget(), validationTuple.getValue());
		}
	}

	public enum On {
		targetAndValue,
		target
	}
}
