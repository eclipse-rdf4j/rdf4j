/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;

/**
 * @author Håvard Ottestad
 */
public class ExternalPredicateObjectFilter implements PlanNode {

	private final SailConnection connection;
	private final Set<Resource> filterOnObject;
	private final IRI filterOnPredicate;
	PlanNode parent;
	int index = 0;
	private final boolean returnMatching;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ExternalPredicateObjectFilter(SailConnection connection, IRI filterOnPredicate, Set<Resource> filterOnObject,
			PlanNode parent, int index,
			boolean returnMatching) {
		this.connection = connection;
		this.filterOnPredicate = filterOnPredicate;
		this.filterOnObject = filterOnObject;
		this.parent = parent;
		this.index = index;
		this.returnMatching = returnMatching;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			Tuple next = null;

			final CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			void calculateNext() {
				while (next == null && parentIterator.hasNext()) {
					Tuple temp = parentIterator.next();

					Value subject = temp.getLine().get(index);

					Resource matchedType = isType(subject);

					if (returnMatching) {
						if (matchedType != null) {
							next = temp;
							next.addHistory(new Tuple(Arrays.asList(subject, filterOnPredicate, matchedType)));
						} else {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								validationExecutionLogger.log(depth(),
										ExternalPredicateObjectFilter.this.getClass().getSimpleName()
												+ ":IgnoredAsTypeMismatch",
										temp, ExternalPredicateObjectFilter.this,
										getId());
							}
						}
					} else {
						if (matchedType == null) {
							next = temp;
							next.addHistory(new Tuple(Arrays.asList(subject)));
						} else {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								validationExecutionLogger.log(depth(),
										ExternalPredicateObjectFilter.this.getClass().getSimpleName()
												+ ":IgnoredAsTypeMismatch",
										temp, ExternalPredicateObjectFilter.this,
										getId());
							}
						}
					}

				}
			}

			private Resource isType(Value subject) {
				if (subject instanceof Resource) {
					return filterOnObject.stream()
							.filter(type -> connection.hasStatement((Resource) subject, filterOnPredicate, type, true))
							.findFirst()
							.orElse(null);
				}
				return null;
			}

			@Override
			public void close() throws SailException {
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

		if (connection instanceof MemoryStoreConnection) {
			stringBuilder.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> "
					+ getId() + " [label=\"filter source\"]").append("\n");
		} else {
			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId() + " [label=\"filter source\"]")
					.append("\n");
		}

		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "ExternalPredicateObjectFilter{" +
				", filterOnPredicate=" + filterOnPredicate +
				"filterOnObject=" + Arrays.toString(filterOnObject.stream().map(Formatter::prefix).toArray()) +
				", index=" + index +
				'}';
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
