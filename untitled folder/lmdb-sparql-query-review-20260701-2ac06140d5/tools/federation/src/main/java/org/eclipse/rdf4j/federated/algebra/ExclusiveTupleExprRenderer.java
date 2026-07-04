/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A specialization of {@link ExclusiveTupleExpr} which provides definitions how the expressions can be rendered to a
 * sub-query.
 *
 * <p>
 * This is required for the evaluation of sub queries.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public interface ExclusiveTupleExprRenderer extends ExclusiveTupleExpr {

	/**
	 * Returns a SPARQL string representation of this expression that can be inserted into a SELECT query.
	 * <p>
	 * Implementations are required to create a valid query string for this expression where the given bindings are
	 * inserted.
	 * </p>
	 *
	 * @param varNames the set of resulting (unbound) variables from this expression
	 * @param bindings the optional input bindings
	 * @return the query string part
	 */
	String toQueryString(Set<String> varNames, BindingSet bindings);

	/**
	 * Returns a SPARQL algebra representation of this expression that can be inserted into a SELECT {@link TupleExpr}
	 * <p>
	 * Implementations are required to create a new equivalent expression or clone, where any provided input bindings
	 * are inserted. The free variable names after insertion need to be added to the provided set.
	 * </p>
	 *
	 * @param varNames the set of resulting (unbound) variables from this expression
	 * @param bindings the input bindings that need to be inserted
	 * @return the algebra expression
	 */
	TupleExpr toQueryAlgebra(Set<String> varNames, BindingSet bindings);
}
