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
package org.eclipse.rdf4j.query.parser;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Abstract super class of all query types that a query parser can generate.
 *
 * @author Arjohn Kampman
 */
public abstract class ParsedQuery extends ParsedOperation {

	/*-----------*
	 * Variables *
	 *-----------*/

	private TupleExpr tupleExpr;

	/**
	 * The dataset that was specified in the operation, if any.
	 */
	private Dataset dataset;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new query object. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 */
	protected ParsedQuery() {
		super();
	}

	/**
	 * Creates a new query object. To complete this query, a tuple expression needs to be supplied to it using
	 * {@link #setTupleExpr(TupleExpr)}.
	 */
	protected ParsedQuery(String sourceString) {
		super(sourceString);
	}

	/**
	 * Creates a new query object.
	 *
	 * @param tupleExpr The tuple expression underlying this query.
	 */
	protected ParsedQuery(String sourceString, TupleExpr tupleExpr) {
		this(sourceString);
		setTupleExpr(tupleExpr);
	}

	/**
	 * Creates a new query object.
	 *
	 * @param tupleExpr The tuple expression underlying this query.
	 */
	protected ParsedQuery(TupleExpr tupleExpr) {
		this(null, tupleExpr);
	}

	/**
	 * Creates a new query object.
	 *
	 * @param tupleExpr The tuple expression underlying this query.
	 */
	protected ParsedQuery(TupleExpr tupleExpr, Dataset dataset) {
		this(null, tupleExpr, dataset);
	}

	/**
	 * Creates a new query object.
	 *
	 * @param tupleExpr The tuple expression underlying this query.
	 */
	protected ParsedQuery(String sourceString, TupleExpr tupleExpr, Dataset dataset) {
		this(sourceString, tupleExpr);
		setDataset(dataset);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	/**
	 * Gets the tuple expression underlying this operation.
	 */
	public void setTupleExpr(TupleExpr tupleExpr) {
		assert tupleExpr != null : "tupleExpr must not be null";
		this.tupleExpr = tupleExpr;
	}

	/**
	 * Gets the tuple expression underlying this operation.
	 */
	public TupleExpr getTupleExpr() {
		return tupleExpr;
	}

	@Override
	public String toString() {
		if (getDataset() != null) {
			return getDataset().toString() + getTupleExpr().toString();
		} else {
			return getTupleExpr().toString();
		}
	}

}
