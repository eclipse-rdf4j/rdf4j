package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class Select implements PlanNode {

	final Repository repository;
	ShaclSailConnection connection;

	String query;

	public Select(Repository repository, String query) {
		this.repository = repository;
		this.query = "select * where { " + query + "} order by ?a";
	}

	public Select(ShaclSailConnection connection, String query) {
		this.connection = connection;
		this.repository = null;
		this.query = "select * where { " + query + "} order by ?a";
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			TupleQueryResult evaluate;
			RepositoryConnection repositoryConnection;


			{
				if (repository != null && connection == null) {
					repositoryConnection = repository.getConnection();
					evaluate = repositoryConnection.prepareTupleQuery(query).evaluate();
				} else {
//					ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, "");
//					TupleExpr tupleExpr = parsedQuery.getTupleExpr();
//
//					evaluate = null;

					throw new UnsupportedOperationException();
				}
			}


			@Override
			public void close() throws SailException {
				try {
					if (evaluate != null) {
						evaluate.close();
					}
				} finally {
					if (repositoryConnection != null) {
						repositoryConnection.close();
					}
				}

			}

			@Override
			public boolean hasNext() throws SailException {
				return evaluate.hasNext();
			}

			@Override
			public Tuple next() throws SailException {
				return new Tuple(evaluate.next());
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return 0;
	}


}
