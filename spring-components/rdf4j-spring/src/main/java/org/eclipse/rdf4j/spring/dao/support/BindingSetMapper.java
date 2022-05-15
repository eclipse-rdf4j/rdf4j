/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support;

import java.util.function.Function;

import org.eclipse.rdf4j.query.BindingSet;

/**
 * Maps a query solution to an instance.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 * @param <T>
 */
public interface BindingSetMapper<T> extends Function<BindingSet, T> {

	/**
	 * Maps a query solution to an instance of <code>T</code>. If the return value is <code>null</code> the mapper
	 * indicates that the solution is to be disregarded.
	 *
	 * @return an instance of <code>T</code> or null if the solution should be ignored.
	 */
	@Override
	T apply(BindingSet bindings);

	static BindingSetMapper<BindingSet> identity() {
		return b -> b;
	}
}
