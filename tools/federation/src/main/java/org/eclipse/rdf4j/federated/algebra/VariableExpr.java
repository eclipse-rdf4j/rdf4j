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

import java.util.List;

import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;

/**
 * Interface for algebra nodes that can return the free variables of the expression.
 *
 * @author Andreas Schwarte
 * @see QueryAlgebraUtil#getFreeVars(org.eclipse.rdf4j.query.algebra.TupleExpr)
 */
public interface VariableExpr {

	/**
	 * @return a list of free (i.e. unbound) variables in this expression
	 */
	List<String> getFreeVars();
}
