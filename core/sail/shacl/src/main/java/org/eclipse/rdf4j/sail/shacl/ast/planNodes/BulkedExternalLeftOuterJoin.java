/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayDeque;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;

/**
 * @author Håvard Ottestad
 *         <p>
 *         External means that this plan node can join the iterator from a plan node with an external source (Repository
 *         or SailConnection) based on a query or a predicate.
 */
public class BulkedExternalLeftOuterJoin extends AbstractBulkJoinPlanNode {

	private final SailConnection connection;
	private final PlanNode leftNode;
	private ParsedQuery parsedQuery;
	private final boolean skipBasedOnPreviousConnection;
	private final SailConnection previousStateConnection;
	private final String query;
	private boolean printed = false;

	public BulkedExternalLeftOuterJoin(PlanNode leftNode, SailConnection connection, String query,
			boolean skipBasedOnPreviousConnection, SailConnection previousStateConnection,
			Function<BindingSet, ValidationTuple> mapper) {
		leftNode = PlanNodeHelper.handleSorting(this, leftNode);
		this.leftNode = leftNode;
		this.query = query;
		this.connection = connection;
		this.skipBasedOnPreviousConnection = skipBasedOnPreviousConnection;
		this.previousStateConnection = previousStateConnection;
		this.mapper = mapper;

	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final ArrayDeque<ValidationTuple> left = new ArrayDeque<>();

			final ArrayDeque<ValidationTuple> right = new ArrayDeque<>();

			final CloseableIteration<? extends ValidationTuple, SailException> leftNodeIterator = leftNode.iterator();

			private void calculateNext() {

				if (!left.isEmpty()) {
					return;
				}

				while (left.size() < 200 && leftNodeIterator.hasNext()) {
					left.addFirst(leftNodeIterator.next());
				}

				if (left.isEmpty()) {
					return;
				}

				if (parsedQuery == null) {
					parsedQuery = parseQuery(query);
				}

				runQuery(left, right, connection, parsedQuery, skipBasedOnPreviousConnection, previousStateConnection,
						mapper);

			}

			@Override
			public void close() throws SailException {
				leftNodeIterator.close();
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return !left.isEmpty();
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();

				if (!left.isEmpty()) {

					ValidationTuple leftPeek = left.peekLast();

					ValidationTuple joined = null;

					if (!right.isEmpty()) {
						ValidationTuple rightPeek = right.peekLast();

						if (rightPeek.sameTargetAs(leftPeek)) {
							// we have a join !
							joined = ValidationTupleHelper.join(leftPeek, rightPeek);
							right.removeLast();

							ValidationTuple rightPeek2 = right.peekLast();

							if (rightPeek2 == null || !rightPeek2.sameTargetAs(leftPeek)) {
								// no more to join from right, pop left so we don't print it again.

								left.removeLast();
							}

						}

					}

					if (joined != null) {
						return joined;
					} else {
						left.removeLast();
						return leftPeek;
					}

				}

				return null;
			}

		};
	}

	@Override
	public int depth() {
		return leftNode.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;

		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");

		leftNode.getPlanAsGraphvizDot(stringBuilder);

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			stringBuilder.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> "
					+ getId() + " [label=\"right\"]").append("\n");
		} else {
			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId() + " [label=\"right\"]")
					.append("\n");
		}

		if (skipBasedOnPreviousConnection) {
			stringBuilder
					.append(System.identityHashCode(previousStateConnection) + " -> " + getId()
							+ " [label=\"skip if not present\"]")
					.append("\n");

		}

		stringBuilder.append(leftNode.getId() + " -> " + getId() + " [label=\"left\"]").append("\n");

	}

	@Override
	public String toString() {
		return "BulkedExternalLeftOuterJoin{" + "query=" + query.replace("\n", "  ")
				+ '}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		leftNode.receiveLogger(validationExecutionLogger);
	}
}
