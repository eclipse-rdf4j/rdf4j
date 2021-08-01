package org.eclipse.rdf4j.spring.dao.support;

import java.util.function.Function;

import org.eclipse.rdf4j.query.TupleQueryResult;

public interface TupleQueryResultMapper<T> extends Function<TupleQueryResult, T> {
}
