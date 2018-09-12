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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 *
 * External means that this plan node can join the iterator from a plan node with an external
 * source (Repository or NotifyingSailConnection) based on a query or a predicate.
 *
 */
public class BulkedExternalLeftOuterJoin implements PlanNode {

	private IRI predicate;
	NotifyingSailConnection baseSailConnection;
	PlanNode leftNode;
	Repository repository;
	String query;

	public BulkedExternalLeftOuterJoin(PlanNode leftNode, Repository repository, String query) {
		this.leftNode = leftNode;
		this.repository = repository;
		this.query = query;
	}

	public BulkedExternalLeftOuterJoin(PlanNode leftNode, Repository repository, IRI predicate) {
		this.leftNode = leftNode;
		this.repository = repository;
		this.predicate = predicate;
	}

	public BulkedExternalLeftOuterJoin(PlanNode leftNode, NotifyingSailConnection baseSailConnection, String query) {
		this.leftNode = leftNode;
		this.query = query;

		this.baseSailConnection = baseSailConnection;

	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			LinkedList<Tuple> left = new LinkedList<>();

			LinkedList<Tuple> right = new LinkedList<>();

			CloseableIteration<Tuple, SailException> leftNodeIterator = leftNode.iterator();


			private void calculateNext() {

				if (!left.isEmpty()) {
					return;
				}


				while (left.size() < 100 && leftNodeIterator.hasNext()) {
					left.addFirst(leftNodeIterator.next());
				}


				if (left.isEmpty()) {
					return;
				}
				if (query != null) {

					StringBuilder newQuery = new StringBuilder("select * where { VALUES (?a) { \n");

					left.stream().map(tuple -> tuple.line.get(0)).map(v -> (Resource) v).forEach(r -> newQuery.append("( <").append(r.toString()).append("> )\n"));

					newQuery.append("\n}")
						.append(query)
						.append("} order by ?a");

					if (repository != null) {
						try (RepositoryConnection connection = repository.getConnection()) {
							connection.begin(IsolationLevels.NONE);

							try (Stream<BindingSet> stream = Iterations.stream(connection.prepareTupleQuery(newQuery.toString()).evaluate())) {
								stream.map(Tuple::new).forEach(right::addFirst);
							}
							connection.commit();
						}
					} else {

						QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance().get(QueryLanguage.SPARQL).get();

						ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(newQuery.toString(), null);

						try (CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate = baseSailConnection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(), new MapBindingSet(), true)) {
							while (evaluate.hasNext()) {
								BindingSet next = evaluate.next();
								right.addFirst(new Tuple(next));
							}
						}

					}
				} else {
					try (RepositoryConnection connection = repository.getConnection()) {
						connection.begin(IsolationLevels.NONE);

						for (Tuple tuple : left) {
							try (Stream<Statement> stream = Iterations.stream(connection.getStatements((Resource) tuple.line.get(0), predicate, null))) {
								stream.forEach(next -> right.addFirst(new Tuple(Arrays.asList(next.getSubject(), next.getObject()))));
							}
						}

						connection.commit();
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

						if (rightPeek.line.get(0) == leftPeek.line.get(0) || rightPeek.line.get(0).equals(leftPeek.line.get(0))) {
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
	public void printPlan() {
		System.out.println(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];");

		leftNode.printPlan();


		if(repository != null){
			System.out.println( System.identityHashCode(repository)+" -> "+getId()+ " [label=\"right\"]");
		}
		if(baseSailConnection != null){
			System.out.println( System.identityHashCode(baseSailConnection)+" -> "+getId()+ " [label=\"right\"]");
		}

		System.out.println(leftNode.getId()+" -> "+getId()+ " [label=\"left\"]");


	}

	@Override
	public String toString() {
		return "BulkedExternalLeftOuterJoin{" +
			"predicate=" + predicate +
			", query='" + query + '\'' +
			'}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}
}
