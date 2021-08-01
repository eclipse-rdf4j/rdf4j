package org.eclipse.rdf4j.spring.dao.support;

import java.util.function.Function;

import org.eclipse.rdf4j.query.BindingSet;

/**
 * Maps a query solution to an instance.
 *
 * @param <T>
 */
public interface BindingSetMapper<T> extends Function<BindingSet, T> {
	static BindingSetMapper<BindingSet> identity() {
		return b -> b;
	}
}
