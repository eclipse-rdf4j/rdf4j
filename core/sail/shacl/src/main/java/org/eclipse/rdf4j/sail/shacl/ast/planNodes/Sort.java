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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

public class Sort implements PlanNode {

	private final PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public Sort(PlanNode parent) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			List<ValidationTuple> sortedTuples;

			Iterator<ValidationTuple> sortedTuplesIterator;

			boolean closed = false;

			@Override
			public void localClose() throws SailException {
				if (closed) {
					throw new IllegalStateException("Already closed");
				}
				closed = true;

				sortedTuplesIterator = Collections.emptyIterator();
				sortedTuples = null;
			}

			@Override
			protected boolean localHasNext() throws SailException {
				sortTuples();
				return sortedTuplesIterator.hasNext();
			}

			private void sortTuples() {
				if (closed) {
					throw new IllegalStateException("Tried to iterate on a closed iterator");
				}

				if (sortedTuples == null) {
					boolean alreadySorted;
					try (CloseableIteration<? extends ValidationTuple, SailException> iterator = parent.iterator()) {
						sortedTuples = new ArrayList<>(1);
						alreadySorted = true;
						ValidationTuple prev = null;
						while (iterator.hasNext()) {
							ValidationTuple next = iterator.next();
							sortedTuples.add(next);

							// quick break out if sortedTuples is guaranteed to be of size 1 since we don't need to sort
							// it then
							if (sortedTuples.size() == 1 && !iterator.hasNext()) {
								sortedTuplesIterator = sortedTuples.iterator();
								return;
							}

							if (prev != null && prev.compareActiveTarget(next) > 0) {
								alreadySorted = false;
							}
							prev = next;
						}

						assert !iterator.hasNext() : "Iterator: " + iterator;
					}

					if (!alreadySorted && sortedTuples.size() > 1) {
						if (sortedTuples.size() > 8192) { // MIN_ARRAY_SORT_GRAN in Arrays.parallelSort(...)
							ValidationTuple[] objects = sortedTuples.toArray(new ValidationTuple[0]);
							Arrays.parallelSort(objects, ValidationTuple::compareActiveTarget);
							sortedTuples = Arrays.asList(objects);
						} else {
							sortedTuples.sort(ValidationTuple::compareActiveTarget);
						}
					}
					sortedTuplesIterator = sortedTuples.iterator();

				}

			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				sortTuples();

				return sortedTuplesIterator.next();
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Sort)) {
			return false;
		}
		Sort sort = (Sort) o;
		return parent.equals(sort.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
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
		return false;
	}

	@Override
	public String toString() {
		return "Sort{" + "parent=" + parent + '}';
	}
}
