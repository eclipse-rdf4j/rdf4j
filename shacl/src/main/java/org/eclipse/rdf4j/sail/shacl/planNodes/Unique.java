/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Håvard Ottestad
 */
public class Unique implements PlanNode {
	private final Logger logger = LoggerFactory.getLogger(Unique.class);

	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public Unique(PlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		Unique that = this;

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			Set<Tuple> multiCardinalityDedupeSet;

			boolean useMultiCardinalityDedupeSet;

			Tuple next;
			Tuple previous;

			private void calculateNext() {
				if (next != null) {
					return;
				}

				while (next == null && parentIterator.hasNext()) {
					Tuple temp = parentIterator.next();

					if (temp.line.size() > 1) {
						useMultiCardinalityDedupeSet = true;
					}

					if (previous == null) {
						next = temp;
					} else {
						if (useMultiCardinalityDedupeSet) {
							if (multiCardinalityDedupeSet == null || !previous.line.get(0).equals(temp.line.get(0))) {
								multiCardinalityDedupeSet = new HashSet<>();
								if (previous.line.get(0).equals(temp.line.get(0))) {
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
									that.getClass().getSimpleName() + ":IgnoredNotUnique", temp, that, getId());
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
			Tuple loggingNext() throws SailException {
				calculateNext();

				Tuple temp = next;
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
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}
}
