/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iterator.CloseableIterationIterator;

/**
 * Super type of all query result types (TupleQueryResult, GraphQueryResult, etc.).
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public interface QueryResult<T> extends CloseableIteration<T, QueryEvaluationException>, Iterable<T> {

	@Override
	default Iterator<T> iterator() {
		return new CloseableIterationIterator<>(this);
	}

}
