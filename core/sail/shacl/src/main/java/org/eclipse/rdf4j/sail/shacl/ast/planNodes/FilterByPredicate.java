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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

/**
 * @author Håvard Ottestad
 */
public class FilterByPredicate implements PlanNode {

	private final SailConnection connection;
	private final Set<IRI> filterOnPredicates;
	final PlanNode parent;
	private final On on;
	private final ConnectionsGroup connectionsGroup;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;
	private final Resource[] dataGraph;

	public enum On {
		Subject,
		Object
	}

	public FilterByPredicate(SailConnection connection, Set<IRI> filterOnPredicates, PlanNode parent,
			On on, Resource[] dataGraph, ConnectionsGroup connectionsGroup) {
		this.dataGraph = dataGraph;
		this.parent = PlanNodeHelper.handleSorting(this, parent, connectionsGroup);
		this.connection = connection;
		assert this.connection != null;
		this.filterOnPredicates = filterOnPredicates;
		this.on = on;
		this.connectionsGroup = connectionsGroup;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			ValidationTuple next = null;

			private CloseableIteration<? extends ValidationTuple> parentIterator;

			List<IRI> filterOnPredicates = null;

			@Override
			protected void init() {
				parentIterator = parent.iterator();
			}

			void calculateNext() {

				if (Thread.currentThread().isInterrupted()) {
					close();
					return;
				}

				if (filterOnPredicates == null) {
					if (!parentIterator.hasNext()) {
						return;
					}

					filterOnPredicates = FilterByPredicate.this.filterOnPredicates
							.stream()
							.map(iri -> connectionsGroup.getSailSpecificValue(iri,
									ConnectionsGroup.StatementPosition.predicate, connection
							))
							.collect(Collectors.toList());

				}

				if (filterOnPredicates.isEmpty()) {
					return;
				}

				while (next == null && parentIterator.hasNext()) {
					if (Thread.currentThread().isInterrupted()) {
						close();
						return;
					}

					ValidationTuple temp = parentIterator.next();

					Value subject = temp.getActiveTarget();

					IRI matchedPredicate = matchesFilter(subject);

					if (matchedPredicate != null) {
						next = temp;
					}

				}
			}

			private IRI matchesFilter(Value node) {

				if (node.isResource() && on == On.Subject) {

					return filterOnPredicates.stream()
							.filter(predicate -> connection.hasStatement((Resource) node, predicate, null, true,
									dataGraph))
							.findFirst()
							.orElse(null);

				} else if (on == On.Object) {

					return filterOnPredicates.stream()
							.filter(predicate -> connection.hasStatement(null, predicate, node, true, dataGraph))
							.findFirst()
							.orElse(null);

				}
				return null;
			}

			@Override
			public void localClose() {
				if (parentIterator != null) {
					parentIterator.close();
				}
			}

			@Override
			protected boolean localHasNext() {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() {
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
//		if (connection instanceof MemoryStoreConnection) {
//			stringBuilder.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> "
//					+ getId() + " [label=\"filter source\"]").append("\n");
//		} else {
		stringBuilder.append(System.identityHashCode(connection) + " -> " + getId() + " [label=\"filter source\"]")
				.append("\n");
//		}

		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "FilterByPredicate{" + "filterOnPredicates="
				+ Formatter.prefix(filterOnPredicates)
				+ '}';
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FilterByPredicate that = (FilterByPredicate) o;
		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return ((MemoryStoreConnection) connection).getSail()
					.equals(((MemoryStoreConnection) that.connection).getSail()) &&
					Arrays.equals(dataGraph, that.dataGraph)
					&& filterOnPredicates.equals(that.filterOnPredicates) && parent.equals(that.parent)
					&& on == that.on;

		}

		return Objects.equals(connection, that.connection) && filterOnPredicates.equals(that.filterOnPredicates) &&
				Arrays.equals(dataGraph, that.dataGraph)
				&& parent.equals(that.parent) && on == that.on;
	}

	@Override
	public int hashCode() {
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), filterOnPredicates,
					Arrays.hashCode(dataGraph), parent, on);

		}
		return Objects.hash(connection, filterOnPredicates, Arrays.hashCode(dataGraph), parent, on);
	}
}
