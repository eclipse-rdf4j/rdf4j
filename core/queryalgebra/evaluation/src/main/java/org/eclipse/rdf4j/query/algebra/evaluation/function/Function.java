/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A query function, which can be a built-in function in the query language, or a custom function as documented in the
 * <a href="https://www.w3.org/TR/sparql11-query/">SPARQL 1.1 Query Language Recommendation</a>.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 * @see FunctionRegistry
 */
public interface Function {

	String getURI();

	/**
	 * Evaluate the function over the supplied input arguments, using the supplied {@link ValueFactory} to produce the
	 * result.
	 *
	 * @param valueFactory a {@link ValueFactory} to use for producing the function result.
	 * @param args         the function input arguments.
	 * @return the function result value.
	 * @throws ValueExprEvaluationException
	 * @deprecated since 3.3.0. Use {@link #evaluate(TripleSource, Value...)} instead. A reference to a ValueFactory can
	 *             be retrieved using {@link TripleSource#getValueFactory()} if needed.
	 */
	@Deprecated
	Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException;

	/**
	 * Evaluate the function over the supplied input arguments.
	 *
	 * @param tripleSource the {@link TripleSource} used in the query evaluation. This can be used to access the current
	 *                     state of the store.
	 * @param args         the function input arguments.
	 * @return the function result value.
	 * @throws ValueExprEvaluationException
	 * @since 3.3.0
	 */
	default Value evaluate(TripleSource tripleSource, Value... args) throws ValueExprEvaluationException {
		return evaluate(tripleSource.getValueFactory(), args);
	}

	/**
	 * UUID() and STRUUID() must return a different result for each invocation.
	 *
	 * @see https://www.w3.org/TR/sparql11-query/#func-uuid
	 * @see https://www.w3.org/TR/sparql11-query/#func-struuid
	 * @see https://www.w3.org/TR/sparql11-query/#func-numerics
	 * @return if each invocation must return a different result.
	 */
	default boolean mustReturnDifferentResult() {
		return false;
	}
}
