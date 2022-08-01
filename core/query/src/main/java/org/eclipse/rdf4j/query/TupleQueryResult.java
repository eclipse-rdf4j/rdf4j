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
package org.eclipse.rdf4j.query;

import java.util.List;

/**
 * A representation of a variable-binding query result as a sequence of {@link BindingSet} objects. Each query result
 * consists of zero or more solutions, each of which represents a single query solution as a set of bindings. Note: take
 * care to always close a TupleQueryResult after use to free any resources it keeps hold of.
 *
 * @author jeen
 */
public interface TupleQueryResult extends QueryResult<BindingSet> {

	/**
	 * Gets the names of the bindings, in order of projection.
	 *
	 * @return The binding names, in order of projection.
	 * @throws QueryEvaluationException
	 */
	List<String> getBindingNames() throws QueryEvaluationException;
}
