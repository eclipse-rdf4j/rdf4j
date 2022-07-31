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
package org.eclipse.rdf4j.sail.lucene;

import java.util.Collection;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailException;

public interface SearchQueryInterpreter {

	/**
	 * Processes a TupleExpr into a set of SearchQueryEvaluators.
	 *
	 * @param tupleExpr the TupleExpr to process.
	 * @param bindings  any bindings.
	 * @param specs     the Collection to add any SearchQueryEvaluators to.
	 */
	void process(TupleExpr tupleExpr, BindingSet bindings, Collection<SearchQueryEvaluator> specs) throws SailException;
}
