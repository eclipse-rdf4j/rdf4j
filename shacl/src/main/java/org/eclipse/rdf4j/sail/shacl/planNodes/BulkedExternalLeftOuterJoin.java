package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

public class BulkedExternalLeftOuterJoin implements PlanNode {

	PlanNode parent;
	Repository addedStatements;
	String query;

	public BulkedExternalLeftOuterJoin(PlanNode parent, Repository addedStatements, String query) {
		this.parent = parent;
		this.addedStatements = addedStatements;
		this.query = query;
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

				try (RepositoryConnection connection = addedStatements.getConnection()) {
					try (Stream<BindingSet> stream = Iterations.stream(connection.prepareTupleQuery(newQuery.toString()).evaluate())) {
						stream.map(Tuple::new).forEach(right::push);
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

							if(rightPeek2 == null || !rightPeek2.line.get(0).equals(leftPeek.line.get(0))){
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
}
