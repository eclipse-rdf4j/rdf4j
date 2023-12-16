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

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.AvailableStatementOrder;
import org.eclipse.rdf4j.common.order.StatementOrder;
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
public interface TripleSource extends AvailableStatementOrder {

	EmptyIteration<? extends Statement> EMPTY_ITERATION = new EmptyIteration<>();

	/**
	 * Gets all statements that have a specific subject, predicate and/or object. All three parameters may be null to
	 * indicate wildcards. Optionally a (set of) context(s) may be specified in which case the result will be restricted
	 * to statements matching one or more of the specified contexts.
	 *
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) to get the statements from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return An iterator over the relevant statements.
	 * @throws QueryEvaluationException If the triple source failed to get the statements.
	 */
	CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException;

	/**
	 * Gets all statements that have a specific subject, predicate and/or object. All three parameters may be null to
	 * indicate wildcards. Optionally a (set of) context(s) may be specified in which case the result will be restricted
	 * to statements matching one or more of the specified contexts.
	 * <p>
	 * Statements are returned in the order specified by the <var>statementOrder</var> parameter. Use
	 * {@link #getSupportedOrders(Resource, IRI, Value, Resource...)} to first retrieve the statement orders supported
	 * by this store for this statement pattern.
	 * <p>
	 * Note that this method is experimental and may be changed or removed without notice.
	 *
	 * @param order    The order in which the statements should be returned.
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) to get the statements from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are supplied the method operates on the entire repository.
	 * @return An ordered iterator over the relevant statements.
	 * @throws QueryEvaluationException If the triple source failed to get the statements.
	 */
	@Experimental
	default CloseableIteration<? extends Statement> getStatements(StatementOrder order, Resource subj, IRI pred,
			Value obj, Resource... contexts) throws QueryEvaluationException {
		throw new UnsupportedOperationException(
				"StatementOrder is not supported by this TripleSource: " + this.getClass().getName());
	}

	/**
	 * The underlying store may support some, but not all, statement orders based on the statement pattern. This method
	 * can be used to determine which orders are supported for a given statement pattern. The supported orders can be
	 * used to retrieve statements in a specific order using
	 * {@link #getStatements(StatementOrder, Resource, IRI, Value, Resource...)} .
	 * <p>
	 * Note that this method is experimental and may be changed or removed without notice.
	 *
	 * @param subj     A Resource specifying the subject, or <var>null</var> for a wildcard.
	 * @param pred     A IRI specifying the predicate, or <var>null</var> for a wildcard.
	 * @param obj      A Value specifying the object, or <var>null</var> for a wildcard.
	 * @param contexts The context(s) to get the data from. Note that this parameter is a vararg and as such is
	 *                 optional. If no contexts are specified the method operates on the entire repository. A
	 *                 <var>null</var> value can be used to match context-less statements.
	 * @return a set of supported statement orders
	 */
	@Experimental
	default Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return Set.of();
	}

	/**
	 * Different underlying datastructures may have different ways of ordering statements. On-disk stores typically use
	 * a long to represent a value and only stores the actual value in a dictionary, in this case the order would be the
	 * order that values where inserted into the dictionary. Stores that instead store values in SPARQL-order can return
	 * an instance of {@link org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator} which may allow for
	 * further optimizations.
	 * <p>
	 * Note that this method is experimental and may be changed or removed without notice.
	 *
	 * @return a comparator that matches the order of values in the store
	 */
	@Experimental
	default Comparator<Value> getComparator() {
		return null;
	}

	/**
	 * Gets a ValueFactory object that can be used to create IRI-, blank node- and literal objects.
	 *
	 * @return a ValueFactory object for this TripleSource.
	 */
	ValueFactory getValueFactory();
}
