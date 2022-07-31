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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;

/**
 * @author Håvard Ottestad
 */
public class ExternalPredicateObjectFilter implements PlanNode {

	private final SailConnection connection;
	private final Set<Resource> filterOnObject;
	private final IRI filterOnPredicate;
	private final FilterOn filterOn;
	PlanNode parent;
	private final boolean returnMatching;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;
	private final Resource[] dataGraph;

	public ExternalPredicateObjectFilter(SailConnection connection, Resource[] dataGraph, IRI filterOnPredicate,
			Set<Resource> filterOnObject, PlanNode parent, boolean returnMatching, FilterOn filterOn) {
		this.dataGraph = dataGraph;
		this.parent = PlanNodeHelper.handleSorting(this, parent);

		this.connection = connection;
		assert this.connection != null;
		this.filterOnPredicate = filterOnPredicate;
		this.filterOnObject = filterOnObject;
		this.filterOn = filterOn;
		this.returnMatching = returnMatching;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			ValidationTuple next = null;

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();

			List<Resource> filterOnObject = null;
			IRI filterOnPredicate = null;

			void calculateNext() {

				if (filterOnObject == null) {
					if (!parentIterator.hasNext()) {
						return;
					}

					try (var stream = connection
							.getStatements(null, ExternalPredicateObjectFilter.this.filterOnPredicate, null, true,
									dataGraph)
							.stream()) {
						filterOnPredicate = stream.map(Statement::getPredicate).findAny().orElse(null);
					}

					if (filterOnPredicate == null) {
						filterOnObject = Collections.emptyList();
					} else {
						filterOnObject = ExternalPredicateObjectFilter.this.filterOnObject.stream()
								.map(object -> {
									try (var stream = connection
											.getStatements(null, filterOnPredicate, object, true, dataGraph)
											.stream()) {
										return stream.map(Statement::getObject)
												.map(o -> ((Resource) o))
												.findAny()
												.orElse(null);
									}
								}
								)
								.filter(Objects::nonNull)
								.collect(Collectors.toList());
					}

				}

				if (returnMatching && (filterOnPredicate == null || filterOnObject.isEmpty())) {
					return;
				}

				while (next == null && parentIterator.hasNext()) {
					ValidationTuple temp = parentIterator.next();

					Value value;
					switch (filterOn) {
					case value:
						value = temp.getValue();
						break;
					case activeTarget:
						value = temp.getActiveTarget();
						break;
					default:
						throw new IllegalStateException("Unknown filterOn: " + filterOn);
					}

					boolean matches = matches(value, filterOnPredicate, filterOnObject);

					if (returnMatching) {
						if (matches) {
							next = temp;
						} else {
							if (validationExecutionLogger.isEnabled()) {
								validationExecutionLogger.log(depth(),
										ExternalPredicateObjectFilter.this.getClass().getSimpleName()
												+ ":IgnoredAsNotMatching",
										temp, ExternalPredicateObjectFilter.this, getId(), null);
							}
						}
					} else {
						if (!matches) {
							next = temp;
						} else {
							if (validationExecutionLogger.isEnabled()) {
								validationExecutionLogger.log(depth(),
										ExternalPredicateObjectFilter.this.getClass().getSimpleName()
												+ ":IgnoredAsMatching",
										temp, ExternalPredicateObjectFilter.this, getId(), null);
							}
						}
					}

				}

				assert next != null || !parentIterator.hasNext() : parentIterator.toString();
			}

			private boolean matches(Value subject, IRI filterOnPredicate, Collection<Resource> filterOnObject) {
				if (filterOnPredicate == null || filterOnObject.isEmpty()) {
					return false;
				}

				if (subject.isResource()) {
					return filterOnObject.stream()
							.anyMatch(object -> connection.hasStatement((Resource) subject, filterOnPredicate, object,
									true, dataGraph));
				}
				return false;
			}

			@Override
			public void localClose() throws SailException {
				parentIterator.close();
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();

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

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
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
		return parent.producesSorted();
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	public enum FilterOn {
		activeTarget,
		value
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExternalPredicateObjectFilter that = (ExternalPredicateObjectFilter) o;
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return returnMatching == that.returnMatching
					&& ((MemoryStoreConnection) connection).getSail()
							.equals(((MemoryStoreConnection) that.connection).getSail())
					&& filterOnObject.equals(that.filterOnObject) && filterOnPredicate.equals(that.filterOnPredicate)
					&& filterOn == that.filterOn && Arrays.equals(dataGraph, that.dataGraph)
					&& parent.equals(that.parent);
		} else {
			return returnMatching == that.returnMatching && Objects.equals(connection, that.connection)
					&& filterOnObject.equals(that.filterOnObject) && filterOnPredicate.equals(that.filterOnPredicate)
					&& filterOn == that.filterOn && Arrays.equals(dataGraph, that.dataGraph)
					&& parent.equals(that.parent);
		}
	}

	@Override
	public int hashCode() {
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), filterOnObject, filterOnPredicate,
					filterOn, parent, returnMatching, Arrays.hashCode(dataGraph));

		} else {
			return Objects.hash(connection, filterOnObject, filterOnPredicate, filterOn, parent, returnMatching,
					Arrays.hashCode(dataGraph));
		}
	}

	@Override
	public String toString() {
		return "ExternalPredicateObjectFilter{" + "filterOnObject=" + filterOnObject + ", filterOnPredicate="
				+ filterOnPredicate + ", filterOn=" + filterOn + ", parent=" + parent + ", returnMatching="
				+ returnMatching + '}';
	}
}
