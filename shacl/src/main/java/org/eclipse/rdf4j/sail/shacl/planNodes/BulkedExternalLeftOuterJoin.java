package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.stream.Stream;

public class BulkedExternalLeftOuterJoin implements PlanNode {

	ShaclSailConnection shaclSailConnection;
	PlanNode parent;
	Repository repository;
	String query;

	public BulkedExternalLeftOuterJoin(PlanNode parent, Repository repository, String query) {
		this.parent = parent;
		this.repository = repository;
		this.query = query;
	}

	public BulkedExternalLeftOuterJoin(PlanNode parent, ShaclSailConnection shaclSailConnection, String query) {
		this.parent = parent;
		this.query = query;

		this.shaclSailConnection = shaclSailConnection;

	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			Deque<Tuple> left = new ArrayDeque<>(101);

			Deque<Tuple> right = new ArrayDeque<>(101);

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();


			private void calculateNext() {

				if (!left.isEmpty()) {
					return;
				}


				while (left.size() < 100 && parentIterator.hasNext()) {
					left.push(parentIterator.next());
				}


				if (left.isEmpty()) {
					return;
				}

				StringBuilder newQuery = new StringBuilder("select * where { VALUES (?a) { \n");

				left.stream().map(tuple -> tuple.line.get(0)).map(v -> (Resource) v).forEach(r -> newQuery.append("( <").append(r.toString()).append("> )\n"));

				newQuery.append("\n}")
					.append(query)
					.append("} order by ?a");

				if (repository != null) {
					try (RepositoryConnection connection = repository.getConnection()) {
						try (Stream<BindingSet> stream = Iterations.stream(connection.prepareTupleQuery(newQuery.toString()).evaluate())) {
							stream.map(Tuple::new).forEach(right::push);
						}
					}
				}else{

					QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance().get(QueryLanguage.SPARQL).get();

					ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(newQuery.toString(), null);

					try (CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate = shaclSailConnection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(), new MapBindingSet(), true)) {
						while(evaluate.hasNext()){
							BindingSet next = evaluate.next();
							right.push(new Tuple(next));
						}
					}

				}

			}

			@Override
			public void close() throws SailException {
				parentIterator.close();
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

					Tuple leftPeek = left.peek();

					Tuple joined = null;

					if (!right.isEmpty()) {
						Tuple rightPeek = right.peek();

						if (rightPeek.line.get(0).equals(leftPeek.line.get(0))) {
							// we have a join !
							joined = TupleHelper.join(leftPeek, rightPeek);
							right.pop();

							Tuple rightPeek2 = right.peek();

							if (rightPeek2 == null || !rightPeek2.line.get(0).equals(leftPeek.line.get(0))) {
								// no more to join from right, pop left so we don't print it again.

								left.pop();
							}


						}

					}


					if (joined != null) {
						return joined;
					} else {
						left.pop();
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
		return parent.depth()+1;
	}
}
