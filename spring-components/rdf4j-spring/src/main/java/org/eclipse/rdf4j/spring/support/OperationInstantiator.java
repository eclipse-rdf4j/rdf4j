package org.eclipse.rdf4j.spring.support;

import java.util.function.Supplier;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public interface OperationInstantiator {

	TupleQuery getTupleQuery(RepositoryConnection con, String queryString);

	TupleQuery getTupleQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> queryStringSupplier);

	Update getUpdate(RepositoryConnection con, String updateString);

	Update getUpdate(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> updateStringSupplier);

	GraphQuery getGraphQuery(RepositoryConnection con, String graphQuery);

	GraphQuery getGraphQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> graphQuerySupplier);
}
