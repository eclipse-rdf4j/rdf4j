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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author Håvard Ottestad
 */
public class FilterByPredicateObject implements PlanNode {

	static private final Logger logger = LoggerFactory.getLogger(FilterByPredicateObject.class);

	private final SailConnection connection;
	private final boolean includeInferred;
	private final Set<Resource> filterOnObject;
	private final IRI filterOnPredicate;
	private final FilterOn filterOn;
	private final PlanNode parent;
	private final boolean returnMatching;
	private final ConnectionsGroup connectionsGroup;
	private StackTraceElement[] stackTrace;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;
	private final Resource[] dataGraph;
	boolean typeFilterWithInference;

	private final Cache<Resource, Boolean> cache;

	public FilterByPredicateObject(SailConnection connection, Resource[] dataGraph, IRI filterOnPredicate,
			Set<Resource> filterOnObject, PlanNode parent, boolean returnMatching, FilterOn filterOn,
			boolean includeInferred, ConnectionsGroup connectionsGroup) {

		this.dataGraph = dataGraph;
		this.parent = PlanNodeHelper.handleSorting(this, parent, connectionsGroup);
		this.connection = connection;
		assert this.connection != null;
		this.includeInferred = includeInferred;
		this.filterOnPredicate = filterOnPredicate;
		this.filterOnObject = filterOnObject;
		this.filterOn = filterOn;
		this.returnMatching = returnMatching;

		if (connection instanceof MemoryStoreConnection) {
			cache = null;
		} else {
			cache = CacheBuilder.newBuilder().maximumSize(10000).build();
		}

		this.connectionsGroup = connectionsGroup;
		if (includeInferred && connectionsGroup.getRdfsSubClassOfReasoner() != null
				&& RDF.TYPE.equals(filterOnPredicate)) {
			typeFilterWithInference = true;
		}
		if (logger.isDebugEnabled()) {
			this.stackTrace = Thread.currentThread().getStackTrace();
		}
	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			ValidationTuple next = null;

			private CloseableIteration<? extends ValidationTuple> parentIterator;

			Resource[] filterOnObject = null;
			IRI filterOnPredicate = null;

			@Override
			protected void init() {
				parentIterator = parent.iterator();
			}

			void calculateNext() {

				if (!parentIterator.hasNext()) {
					return;
				}

				internResources();

				if (returnMatching && (filterOnPredicate == null || filterOnObject.length == 0)) {
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
										FilterByPredicateObject.this.getClass().getSimpleName()
												+ ":IgnoredAsNotMatching",
										temp, FilterByPredicateObject.this, getId(), null);
							}
						}
					} else {
						if (!matches) {
							next = temp;
						} else {
							if (validationExecutionLogger.isEnabled()) {
								validationExecutionLogger.log(depth(),
										FilterByPredicateObject.this.getClass().getSimpleName()
												+ ":IgnoredAsMatching",
										temp, FilterByPredicateObject.this, getId(), null);
							}
						}
					}

				}

				assert next != null || !parentIterator.hasNext() : parentIterator.toString();
			}

			private void internResources() {
				if (filterOnObject == null) {
					filterOnPredicate = connectionsGroup.getSailSpecificValue(
							FilterByPredicateObject.this.filterOnPredicate,
							ConnectionsGroup.StatementPosition.predicate, connection
					);
					if (filterOnPredicate == null) {
						filterOnObject = new Resource[0];
					} else {
						if (typeFilterWithInference) {
							filterOnObject = FilterByPredicateObject.this.filterOnObject.stream()
									.flatMap(type -> connectionsGroup.getRdfsSubClassOfReasoner()
											.backwardsChain(type)
											.stream())
									.distinct()
									.map(object -> connectionsGroup.getSailSpecificValue(object,
											ConnectionsGroup.StatementPosition.object, connection
									))
									.filter(Objects::nonNull)
									.toArray(Resource[]::new);
						} else {
							filterOnObject = FilterByPredicateObject.this.filterOnObject.stream()
									.map(object -> connectionsGroup.getSailSpecificValue(object,
											ConnectionsGroup.StatementPosition.object, connection
									))
									.filter(Objects::nonNull)
									.toArray(Resource[]::new);
						}
					}

				}
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

	private boolean matches(Value subject, IRI filterOnPredicate, Resource[] filterOnObject) {
		if (filterOnPredicate == null || filterOnObject.length == 0) {
			return false;
		}

		if (subject.isResource()) {

			if (cache == null) {
				return matchesUnCached((Resource) subject, filterOnPredicate, filterOnObject);
			} else {
				return matchesCached((Resource) subject, filterOnPredicate, filterOnObject);
			}

		}
		return false;
	}

	private Boolean matchesCached(Resource subject, IRI filterOnPredicate, Resource[] filterOnObject) {
		try {
			return cache.get(subject, () -> matchesUnCached(subject, filterOnPredicate, filterOnObject));
		} catch (ExecutionException e) {
			if (e.getCause() != null) {
				if (e.getCause() instanceof RuntimeException) {
					throw ((RuntimeException) e.getCause());
				}
				throw new SailException(e.getCause());
			}
			throw new SailException(e);
		}
	}

	private boolean matchesUnCached(Resource subject, IRI filterOnPredicate, Resource[] filterOnObject) {
		for (Resource object : filterOnObject) {
			if (connection.hasStatement(subject, filterOnPredicate, object,
					includeInferred && !typeFilterWithInference, dataGraph)) {
				return true;
			}
		}
		return false;
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
		FilterByPredicateObject that = (FilterByPredicateObject) o;
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
		return "FilterByPredicateObject{" + "filterOnObject=" + Formatter.prefix(filterOnObject)
				+ ", filterOnPredicate="
				+ Formatter.prefix(filterOnPredicate) + ", filterOn=" + filterOn + ", returnMatching="
				+ returnMatching + '}';
	}

}
