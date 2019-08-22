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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;

import java.util.Arrays;
import java.util.Set;

/**
 * @author Håvard Ottestad
 */
public class ExternalTypeFilterNode implements PlanNode {

	private SailConnection connection;
	private Set<Resource> filterOnType;
	PlanNode parent;
	int index = 0;
	private final boolean returnMatching;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ExternalTypeFilterNode(SailConnection connection, Set<Resource> filterOnType, PlanNode parent, int index,
			boolean returnMatching) {
		this.connection = connection;
		this.filterOnType = filterOnType;
		this.parent = parent;
		this.index = index;
		this.returnMatching = returnMatching;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		PlanNode that = this;

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			Tuple next = null;

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			void calculateNext() {
				while (next == null && parentIterator.hasNext()) {
					Tuple temp = parentIterator.next();

					Value subject = temp.line.get(index);

					Resource matchedType = isType(subject);

					if (returnMatching) {
						if (matchedType != null) {
							next = temp;
							next.addHistory(new Tuple(Arrays.asList(subject, RDF.TYPE, matchedType)));
						} else {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								validationExecutionLogger.log(depth(),
										that.getClass().getSimpleName() + ":IgnoredAsTypeMismatch", temp, that,
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
										that.getClass().getSimpleName() + ":IgnoredAsTypeMismatch", temp, that,
										getId());
							}
						}
					}

				}
			}

			private Resource isType(Value subject) {
				if (subject instanceof Resource) {
					return filterOnType.stream()
							.filter(type -> connection.hasStatement((Resource) subject, RDF.TYPE, type, true))
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
		return "ExternalTypeFilterNode{" + "filterOnType="
				+ Arrays.toString(filterOnType.stream().map(Formatter::prefix).toArray()) + '}';
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
