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
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;

import java.util.ArrayDeque;

/**
 * @author Håvard Ottestad
 *         <p>
 *         External means that this plan node can join the iterator from a plan node with an external source (Repository
 *         or SailConnection) based on a query or a predicate.
 */
public class BulkedExternalLeftOuterJoin extends AbstractBulkJoinPlanNode {

	private final SailConnection connection;
	private final PlanNode leftNode;
	private final ParsedQuery parsedQuery;
	private final boolean skipBasedOnPreviousConnection;
	private boolean printed = false;

	public BulkedExternalLeftOuterJoin(PlanNode leftNode, SailConnection connection, String query,
			boolean skipBasedOnPreviousConnection) {
		this.leftNode = leftNode;
		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance().get(QueryLanguage.SPARQL).get();
		parsedQuery = queryParserFactory.getParser()
				.parseQuery("select distinct * where { VALUES (?a) {}" + query + "} order by ?a", null);

		this.connection = connection;
		this.skipBasedOnPreviousConnection = skipBasedOnPreviousConnection;

	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			ArrayDeque<Tuple> left = new ArrayDeque<>();

			ArrayDeque<Tuple> right = new ArrayDeque<>();

			CloseableIteration<Tuple, SailException> leftNodeIterator = leftNode.iterator();

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

				runQuery(left, right, connection, parsedQuery, skipBasedOnPreviousConnection);

			}

			@Override
			public void close() throws SailException {
				leftNodeIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return !left.isEmpty();
			}

			@Override
			public Tuple next() throws SailException {
				calculateNext();

				if (!left.isEmpty()) {

					Tuple leftPeek = left.peekLast();

					Tuple joined = null;

					if (!right.isEmpty()) {
						Tuple rightPeek = right.peekLast();

						if (rightPeek.line.get(0) == leftPeek.line.get(0)
								|| rightPeek.line.get(0).equals(leftPeek.line.get(0))) {
							// we have a join !
							joined = TupleHelper.join(leftPeek, rightPeek);
							right.removeLast();

							Tuple rightPeek2 = right.peekLast();

							if (rightPeek2 == null || !rightPeek2.line.get(0).equals(leftPeek.line.get(0))) {
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

		leftNode.getPlanAsGraphvizDot(stringBuilder);

		if (connection instanceof MemoryStoreConnection) {
			stringBuilder.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> "
					+ getId() + " [label=\"right\"]").append("\n");
		} else {
			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId() + " [label=\"right\"]")
					.append("\n");
		}

		if (skipBasedOnPreviousConnection) {
			if (connection instanceof ShaclSailConnection) {
				NotifyingSailConnection previousStateConnection = ((ShaclSailConnection) connection)
						.getPreviousStateConnection();

				stringBuilder
						.append(System.identityHashCode(previousStateConnection) + " -> " + getId()
								+ " [label=\"skip if not present\"]")
						.append("\n");
			}
		}

		stringBuilder.append(leftNode.getId() + " -> " + getId() + " [label=\"left\"]").append("\n");

	}

	@Override
	public String toString() {
		return "BulkedExternalLeftOuterJoin{" + "parsedQuery=" + parsedQuery.getSourceString() + '}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return leftNode.getIteratorDataType();
	}
}
