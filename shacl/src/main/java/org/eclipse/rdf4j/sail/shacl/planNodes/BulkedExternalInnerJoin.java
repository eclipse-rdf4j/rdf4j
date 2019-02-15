/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 * <p>
 * This inner join algorithm assumes the left iterator is unique for tuple[0], eg. no two tuples have the same value at index 0.
 * The right iterator is allowed to contain duplicates.
 * <p>
 * External means that this plan node can join the iterator from a plan node with an external
 * source (Repository or NotifyingSailConnection) based on a query or a predicate.
 */
public class BulkedExternalInnerJoin implements PlanNode {

	private QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance().get(QueryLanguage.SPARQL).get();


	private NotifyingSailConnection baseSailConnection;
	private PlanNode leftNode;
	private Repository repository;
	private String query;
	private ParsedQuery parsedQuery;


	public BulkedExternalInnerJoin(PlanNode leftNode, Repository repository, String query) {
		this.leftNode = leftNode;
		this.repository = repository;

		this.query = query;
		parsedQuery = queryParserFactory.getParser().parseQuery("select * where { VALUES (?a) {}" + query + "} order by ?a", null);

	}

	public BulkedExternalInnerJoin(PlanNode leftNode, NotifyingSailConnection baseSailConnection, String query) {
		this.leftNode = leftNode;
		this.query = query;
		parsedQuery = queryParserFactory.getParser().parseQuery("select * where { VALUES (?a) {}" + query + "} order by ?a", null);

		this.baseSailConnection = baseSailConnection;

	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			LinkedList<Tuple> left = new LinkedList<>();

			LinkedList<Tuple> right = new LinkedList<>();

			CloseableIteration<Tuple, SailException> leftNodeIterator = leftNode.iterator();


			private void calculateNext() {

				if (repository != null) {
					try (RepositoryConnection connection = repository.getConnection()) {
						boolean empty = !connection.hasStatement((Resource) null, (IRI) null, null, true);
						if (empty) {
							return;
						}
					}
				} else {
					boolean empty = !baseSailConnection.hasStatement((Resource) null, (IRI) null, null, true);
					if (empty) {
						return;
					}
				}


				if (!left.isEmpty()) {
					return;
				}


				while (left.size() < 200 && leftNodeIterator.hasNext()) {
					left.addFirst(leftNodeIterator.next());
				}


				if (left.isEmpty()) {
					return;
				}

				if (query != null) {


					if (repository != null) {

						StringBuilder newQuery = new StringBuilder("select * where { VALUES (?a) { \n");

						left.stream().map(tuple -> tuple.line.get(0)).map(v -> (Resource) v).forEach(r -> newQuery.append("( <").append(r.toString()).append("> )\n"));

						newQuery.append("\n}")
							.append(query)
							.append("} order by ?a");

						try (RepositoryConnection connection = repository.getConnection()) {
							connection.begin(IsolationLevels.NONE);

							try (Stream<BindingSet> stream = Iterations.stream(connection.prepareTupleQuery(newQuery.toString()).evaluate())) {
								stream.map(Tuple::new).forEach(right::addFirst);
							}
							connection.commit();
						}
					} else {
//						parsedQuery = queryParserFactory.getParser().parseQuery("select * where { VALUES (?a) {}" + query + "} order by ?a", null);

						try {
							parsedQuery.getTupleExpr().visitChildren(new AbstractQueryModelVisitor<Exception>() {
								@Override
								public void meet(BindingSetAssignment node) throws Exception {

									List<BindingSet> newBindindingset = left.stream()
										.map(tuple -> tuple.line.get(0))
										.map(v -> (Resource) v)
										.map(r -> new ListBindingSet(Collections.singletonList("a"), Collections.singletonList(r)))
										.collect(Collectors.toList());


									node.setBindingSets(newBindindingset);

								}
							});
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

						try (CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate = baseSailConnection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(), new MapBindingSet(), true)) {
							while (evaluate.hasNext()) {
								BindingSet next = evaluate.next();
								right.addFirst(new Tuple(next));
							}
						}

					}
				}

			}

			@Override
			public void close() throws SailException {
				leftNodeIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return !left.isEmpty() && !right.isEmpty();
			}


			@Override
			public Tuple next() throws SailException {
				calculateNext();


				Tuple joined = null;

				while (joined == null) {

					Tuple leftPeek = left.peekLast();


					if (!right.isEmpty()) {
						Tuple rightPeek = right.peekLast();

						if (rightPeek.line.get(0) == leftPeek.line.get(0) || rightPeek.line.get(0).equals(leftPeek.line.get(0))) {
							// we have a join !
							joined = TupleHelper.join(leftPeek, rightPeek);
							right.removeLast();

							Tuple rightPeek2 = right.peekLast();

							if (rightPeek2 == null || !rightPeek2.line.get(0).equals(leftPeek.line.get(0))) {
								// no more to join from right, pop left so we don't print it again.

								left.removeLast();
							}
						} else {
							int compare = rightPeek.line.get(0).stringValue().compareTo(leftPeek.line.get(0).stringValue());

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
				}

				return joined;


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
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(leftNode.getId() + " -> " + getId() + " [label=\"left\"]").append("\n");
		if (repository != null) {
			stringBuilder.append(System.identityHashCode(repository) + " -> " + getId() + " [label=\"right\"]").append("\n");
		}
		if (baseSailConnection != null) {
			stringBuilder.append(System.identityHashCode(baseSailConnection) + " -> " + getId() + " [label=\"right\"]").append("\n");
		}

		leftNode.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "BulkedExternalInnerJoin{" +
			", query='" + query + '\'' +
			'}';
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
