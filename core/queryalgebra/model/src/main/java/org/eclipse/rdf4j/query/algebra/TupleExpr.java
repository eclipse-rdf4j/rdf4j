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
package org.eclipse.rdf4j.query.algebra;

import java.util.Set;

/**
 * An expression that evaluates to RDF tuples.
 */
public interface TupleExpr extends QueryModelNode {

	/**
	 * Gets the names of the bindings that are, or can be, returned by this tuple expression when it is evaluated.
	 *
	 * @return A set of binding names.
	 */
	Set<String> getBindingNames();

	/**
	 * Gets the names of the bindings that are guaranteed to be present in the results produced by this tuple
	 * expression.
	 *
	 * @return A set of binding names.
	 */
	Set<String> getAssuredBindingNames();

	@Override
	TupleExpr clone();
}
