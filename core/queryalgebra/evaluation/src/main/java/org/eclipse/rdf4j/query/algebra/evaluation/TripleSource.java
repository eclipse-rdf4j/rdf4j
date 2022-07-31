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
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * A triple source that can be queried for (the existence of) certain triples in certain contexts. This interface
 * defines the methods that are needed by the Sail Query Model to be able to evaluate itself.
 */
public interface TripleSource {

	EmptyIteration<? extends Statement, QueryEvaluationException> EMPTY_ITERATION = new EmptyIteration<>();
	EmptyIteration<? extends Triple, QueryEvaluationException> EMPTY_TRIPLE_ITERATION = new EmptyIteration<>();

	/**
	 * Gets all statements that have a specific subject, predicate and/or object. All three parameters may be null to
	 * indicate wildcards. Optionally a (set of) context(s) may be specified in which case the result will be restricted
	 * to statements matching one or more of the specified contexts.
	 *
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A URI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) to get the statements from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return An iterator over the relevant statements.
	 * @throws QueryEvaluationException If the triple source failed to get the statements.
	 */
	CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException;

	/**
	 * Gets a ValueFactory object that can be used to create URI-, blank node- and literal objects.
	 *
	 * @return a ValueFactory object for this TripleSource.
	 */
	ValueFactory getValueFactory();
}
