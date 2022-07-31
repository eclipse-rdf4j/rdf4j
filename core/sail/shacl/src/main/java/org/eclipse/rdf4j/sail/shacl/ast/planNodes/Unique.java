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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.CloseablePeakableIteration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class Unique implements PlanNode {
	private final Logger logger = LoggerFactory.getLogger(Unique.class);
	private final boolean compress;
	private StackTraceElement[] stackTrace;

	private final PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	private Unique(PlanNode parent, boolean compress) {
//		this.stackTrace = Thread.currentThread().getStackTrace();
		PlanNode tempParent = PlanNodeHelper.handleSorting(this, parent);

		if (tempParent instanceof Unique) {
			Unique parentUnique = ((Unique) tempParent);

			tempParent = parentUnique.parent;

			if (!compress) {
				compress = parentUnique.compress;
			}
		}

		this.parent = tempParent;
		this.compress = compress;
	}

	public static PlanNode getInstance(PlanNode parent, boolean compress) {
		if (parent == EmptyNode.getInstance()) {
			return parent;
		}
		return new Unique(parent, compress);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseablePeakableIteration<? extends ValidationTuple, SailException> parentIterator;

			{
				if (compress) {
					parentIterator = new CloseablePeakableIteration<>(
							new TargetAndValueSortIterator(new CloseablePeakableIteration<>(parent.iterator())));
				} else {
					parentIterator = new CloseablePeakableIteration<>(parent.iterator());
				}
			}

			Set<ValidationTupleValueAndActiveTarget> targetAndValueDedupeSet;

			boolean propertyShapeWithValue;

			ValidationTuple next;
			ValidationTuple previous;

			private void calculateNext() {
				if (next != null) {
					return;
				}

				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();

					assert !propertyShapeWithValue
							|| temp.getScope() == ConstraintComponent.Scope.propertyShape && temp.hasValue();

					if (temp.getScope() == ConstraintComponent.Scope.propertyShape && temp.hasValue()) {
						propertyShapeWithValue = true;
					}

					if (compress) {
						Set<ValidationTuple> tuples = new HashSet<>();

						if (propertyShapeWithValue) {

							while (parentIterator.hasNext()
									&& parentIterator.peek().getValue().equals(temp.getValue())
									&& parentIterator.peek().sameTargetAs(temp)) {
								tuples.add(parentIterator.next());
							}
						} else {
							while (parentIterator.hasNext() && parentIterator.peek().sameTargetAs(temp)) {
								tuples.add(parentIterator.next());
							}
						}

						if (tuples.isEmpty()) {
							next = temp;
						} else if (tuples.size() == 1 && tuples.contains(temp)) {
							next = temp;
						} else {
							tuples.add(temp);
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
						if (validationExecutionLogger.isEnabled()) {
							validationExecutionLogger.log(depth(),
									Unique.this.getClass().getSimpleName() + ":IgnoredNotUnique ", temp, Unique.this,
									getId(), stackTrace != null ? stackTrace[2].toString() : null);
						}
					}

				}

			}

			@Override
			public void localClose() throws SailException {
				parentIterator.close();
				targetAndValueDedupeSet = null;
				next = null;
				previous = null;
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();
				assert !(previous != null && next.compareActiveTarget(previous) < 0);

				ValidationTuple temp = next;
				next = null;
				return temp;
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
		return Objects.hash(parent);
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
			ValidationTupleValueAndActiveTarget oValidationTuple = (ValidationTupleValueAndActiveTarget) o;

			if (validationTuple.hasValue() || oValidationTuple.validationTuple.hasValue()) {
				assert validationTuple.hasValue() && oValidationTuple.validationTuple.hasValue();
				return validationTuple.getValue().equals(oValidationTuple.validationTuple.getValue())
						&& validationTuple.getActiveTarget().equals(oValidationTuple.validationTuple.getActiveTarget());
			} else {
				return validationTuple.getActiveTarget().equals(oValidationTuple.validationTuple.getActiveTarget());
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(validationTuple.getActiveTarget(), validationTuple.getValue());
		}
	}

	static class TargetAndValueSortIterator implements CloseableIteration<ValidationTuple, SailException> {

		private final CloseablePeakableIteration<? extends ValidationTuple, SailException> iterator;

		public TargetAndValueSortIterator(
				CloseablePeakableIteration<? extends ValidationTuple, SailException> iterator) {
			this.iterator = iterator;
		}

		private Iterator<ValidationTuple> next = Collections.emptyIterator();

		private void calculateNext() {
			if (next.hasNext()) {
				return;
			}

			if (!iterator.hasNext()) {
				return;
			}

			ArrayList<ValidationTuple> validationTuples = new ArrayList<>();
			ValidationTuple temp = iterator.next();
			if (temp.getScope() == ConstraintComponent.Scope.propertyShape && temp.hasValue()) {
				while (iterator.hasNext() && temp.sameTargetAs(iterator.peek())
						&& iterator.peek().getScope() == ConstraintComponent.Scope.propertyShape
						&& iterator.peek().hasValue()) {
					validationTuples.add(iterator.next());
				}
			}

			if (validationTuples.isEmpty()) {
				next = Collections.singletonList(temp).iterator();
			} else {
				validationTuples.add(temp);

				ValueComparator valueComparator = new ValueComparator();
				validationTuples.sort((a, b) -> valueComparator.compare(a.getValue(), b.getValue()));
				next = validationTuples.iterator();

			}

		}

		@Override
		public void close() throws SailException {
			iterator.close();
		}

		@Override
		public boolean hasNext() throws SailException {
			calculateNext();
			return next.hasNext();
		}

		@Override
		public ValidationTuple next() throws SailException {
			calculateNext();
			return next.next();
		}

		@Override
		public void remove() throws SailException {
			throw new UnsupportedOperationException();
		}

	}

}
