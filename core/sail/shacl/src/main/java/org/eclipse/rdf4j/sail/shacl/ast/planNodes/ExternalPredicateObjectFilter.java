/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;
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
	private final FilterOn filterOn;
	PlanNode parent;
	private final boolean returnMatching;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public ExternalPredicateObjectFilter(SailConnection connection, IRI filterOnPredicate, Set<Resource> filterOnObject,
			PlanNode parent,
			boolean returnMatching, FilterOn filterOn) {
		parent = PlanNodeHelper.handleSorting(this, parent);

		this.connection = connection;
		this.filterOnPredicate = filterOnPredicate;
		this.filterOnObject = filterOnObject;
		this.parent = parent;
		this.filterOn = filterOn;
		this.returnMatching = returnMatching;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			ValidationTuple next = null;

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();

			void calculateNext() {
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

					Resource matchedType = isType(value);

					if (returnMatching) {
						if (matchedType != null) {
							next = temp;
						} else {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								validationExecutionLogger.log(depth(),
										ExternalPredicateObjectFilter.this.getClass().getSimpleName()
												+ ":IgnoredAsTypeMismatch",
										temp, ExternalPredicateObjectFilter.this,
										getId(), null);
							}
						}
					} else {
						if (matchedType == null) {
							next = temp;
						} else {
							if (GlobalValidationExecutionLogging.loggingEnabled) {
								validationExecutionLogger.log(depth(),
										ExternalPredicateObjectFilter.this.getClass().getSimpleName()
												+ ":IgnoredAsTypeMismatch",
										temp, ExternalPredicateObjectFilter.this,
										getId(), null);
							}
						}
					}

				}

				assert next != null || !parentIterator.hasNext() : parentIterator.toString();
			}

			private Resource isType(Value subject) {
				if (subject.isResource()) {
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
			return returnMatching == that.returnMatching &&
					((MemoryStoreConnection) connection).getSail()
							.equals(((MemoryStoreConnection) that.connection).getSail())
					&&
					filterOnObject.equals(that.filterOnObject) &&
					filterOnPredicate.equals(that.filterOnPredicate) &&
					filterOn == that.filterOn &&
					parent.equals(that.parent);
		} else {
			return returnMatching == that.returnMatching &&
					connection.equals(that.connection) &&
					filterOnObject.equals(that.filterOnObject) &&
					filterOnPredicate.equals(that.filterOnPredicate) &&
					filterOn == that.filterOn &&
					parent.equals(that.parent);
		}
	}

	@Override
	public int hashCode() {
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), filterOnObject, filterOnPredicate,
					filterOn, parent, returnMatching);

		} else {
			return Objects.hash(connection, filterOnObject, filterOnPredicate, filterOn, parent, returnMatching);
		}
	}

	@Override
	public String toString() {
		return "ExternalPredicateObjectFilter{" +
				"filterOnObject=" + filterOnObject +
				", filterOnPredicate=" + filterOnPredicate +
				", filterOn=" + filterOn +
				", parent=" + parent +
				", returnMatching=" + returnMatching +
				'}';
	}
}
