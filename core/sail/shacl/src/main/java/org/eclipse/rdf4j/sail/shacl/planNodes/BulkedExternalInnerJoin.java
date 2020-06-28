/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.ArrayDeque;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;

/**
 * @author Håvard Ottestad
 *         <p>
 *         This inner join algorithm assumes the left iterator is unique for tuple[0], eg. no two tuples have the same
 *         value at index 0. The right iterator is allowed to contain duplicates.
 *         <p>
 *         External means that this plan node can join the iterator from a plan node with an external source (Repository
 *         or SailConnection) based on a query or a predicate.
 */
public class BulkedExternalInnerJoin extends AbstractBulkJoinPlanNode {

	private static final ValueComparator VALUE_COMPARATOR = new ValueComparator();
	private final SailConnection connection;
	private final PlanNode leftNode;
	private final ParsedQuery parsedQuery;
	private final boolean skipBasedOnPreviousConnection;
	private final SailConnection previousStateConnection;
	private boolean printed = false;

	public BulkedExternalInnerJoin(PlanNode leftNode, SailConnection connection, String query,
			boolean skipBasedOnPreviousConnection, SailConnection previousStateConnection, String... variables) {
		this.leftNode = leftNode;

		parsedQuery = parseQuery(query);

		this.connection = connection;
		this.skipBasedOnPreviousConnection = skipBasedOnPreviousConnection;
		this.variables = variables;
		this.previousStateConnection = previousStateConnection;

	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final ArrayDeque<Tuple> left = new ArrayDeque<>();

			final ArrayDeque<Tuple> right = new ArrayDeque<>();

			final ArrayDeque<Tuple> joined = new ArrayDeque<>();

			final CloseableIteration<Tuple, SailException> leftNodeIterator = leftNode.iterator();

			private void calculateNext() {

				if (!joined.isEmpty()) {
					return;
				}

				while (joined.isEmpty() && leftNodeIterator.hasNext()) {

					while (left.size() < 200 && leftNodeIterator.hasNext()) {
						left.addFirst(leftNodeIterator.next());
					}

					runQuery(left, right, connection, parsedQuery, skipBasedOnPreviousConnection,
							previousStateConnection, variables);

					while (!right.isEmpty()) {

						Tuple leftPeek = left.peekLast();

						Tuple rightPeek = right.peekLast();

						assert leftPeek != null;
						assert rightPeek != null;

						assert leftPeek.getLine() != null;
						assert rightPeek.getLine() != null;

						if (rightPeek.getLine().get(0) == leftPeek.getLine().get(0)
								|| rightPeek.getLine().get(0).equals(leftPeek.getLine().get(0))) {
							// we have a join !
							joined.addLast(TupleHelper.join(leftPeek, rightPeek));
							right.removeLast();

							Tuple rightPeek2 = right.peekLast();

							if (rightPeek2 == null || !rightPeek2.getLine().get(0).equals(leftPeek.getLine().get(0))) {
								// no more to join from right, pop left so we don't print it again.

								left.removeLast();
							}
						} else {
							int compare = VALUE_COMPARATOR.compare(rightPeek.getLine().get(0),
									leftPeek.getLine().get(0));

							if (compare < 0) {
								if (right.isEmpty()) {
									throw new IllegalStateException();
								}

								right.removeLast();

							} else {
								if (left.isEmpty()) {
									throw new IllegalStateException();
								}
								left.removeLast();

							}
						}

					}

					left.clear();
				}

			}

			@Override
			public void close() throws SailException {
				leftNodeIterator.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();
				return !joined.isEmpty();
			}

			@Override
			Tuple loggingNext() throws SailException {
				calculateNext();
				return joined.removeFirst();

			}

			@Override
			public void remove() throws SailException {

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
		stringBuilder.append(leftNode.getId() + " -> " + getId() + " [label=\"left\"]").append("\n");

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

		leftNode.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "BulkedExternalInnerJoin{" + "parsedQuery=" + parsedQuery.getSourceString().replace("\n", "  ") + '}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return leftNode.getIteratorDataType();
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		leftNode.receiveLogger(validationExecutionLogger);
	}
}
