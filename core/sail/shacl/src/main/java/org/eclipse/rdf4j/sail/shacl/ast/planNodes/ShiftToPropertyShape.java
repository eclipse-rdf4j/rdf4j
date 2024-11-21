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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class ShiftToPropertyShape implements PlanNode {
	static private final Logger logger = LoggerFactory.getLogger(ShiftToPropertyShape.class);

	private StackTraceElement[] stackTrace;
	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ShiftToPropertyShape(PlanNode parent, ConnectionsGroup connectionsGroup) {
		this.parent = PlanNodeHelper.handleSorting(this, parent, connectionsGroup);
		// this.stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			private CloseableIteration<? extends ValidationTuple> parentIterator;
			Iterator<ValidationTuple> iterator = Collections.emptyIterator();

			@Override
			protected void init() {
				parentIterator = parent.iterator();
			}

			public void calculateNext() {
				if (!iterator.hasNext()) {
					if (parentIterator.hasNext()) {
						List<ValidationTuple> validationTuples = parentIterator.next().shiftToPropertyShapeScope();
						iterator = validationTuples.iterator();
					}
				}

			}

			@Override
			public void localClose() {
				if (parentIterator != null) {
					parentIterator.close();
				}
				iterator = Collections.emptyIterator();
			}

			@Override
			protected boolean localHasNext() {
				calculateNext();
				return iterator.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() {
				calculateNext();

				return iterator.next();
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
	public String toString() {
		return "ShiftToPropertyShape";
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
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ShiftToPropertyShape that = (ShiftToPropertyShape) o;
		return parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
	}
}
