/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class Unique implements PlanNode {
	private final Logger logger = LoggerFactory.getLogger(Unique.class);
	private final int id;

	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	static AtomicInteger counter = new AtomicInteger();

	public Unique(PlanNode parent) {
		parent = PlanNodeHelper.handleSorting(this, parent);

		this.parent = parent;
		this.id = counter.incrementAndGet();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();

			Set<ValidationTuple> multiCardinalityDedupeSet;

			boolean useMultiCardinalityDedupeSet;

			ValidationTuple next;
			ValidationTuple previous;

			private void calculateNext() {
				if (next != null) {
					return;
				}

				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();

					if (temp.getFullChainSize() > 1) {
						useMultiCardinalityDedupeSet = true;
					}

					if (previous == null) {
						next = temp;
					} else {
						if (useMultiCardinalityDedupeSet) {
							if (multiCardinalityDedupeSet == null
									|| !previous.sameTargetAs(temp)) {
								multiCardinalityDedupeSet = new HashSet<>();
								if (previous.sameTargetAs(temp)) {
									multiCardinalityDedupeSet.add(previous);
								}
							}

							if (!multiCardinalityDedupeSet.contains(temp)) {
								next = temp;
								multiCardinalityDedupeSet.add(next);
							}

						} else {
							if (!(previous == temp || previous.equals(temp))) {
								next = temp;
							}
						}

					}

					if (next != null) {
						previous = next;
					} else {
						if (GlobalValidationExecutionLogging.loggingEnabled) {
							validationExecutionLogger.log(depth(),
									Unique.this.getClass().getSimpleName() + ":IgnoredNotUnique", temp, Unique.this,
									getId());
						}
					}

				}

			}

			@Override
			public void close() throws SailException {
				multiCardinalityDedupeSet = null;
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
				if (previous != null && next.compareTarget(previous) < 0) {
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
	public String toString() {
		return "Unique";
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
}
