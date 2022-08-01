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

import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;

public interface SearchQueryEvaluator {

	QueryModelNode getParentQueryModelNode();

	/**
	 * @param bsa binding sets, pass null or empty when there are no results for the query
	 */
	void replaceQueryPatternsWithResults(final BindingSetAssignment bsa);

	/**
	 * Removes the query patterns and returns a placeholder where the query results could be placed.
	 */
	QueryModelNode removeQueryPatterns();
}
