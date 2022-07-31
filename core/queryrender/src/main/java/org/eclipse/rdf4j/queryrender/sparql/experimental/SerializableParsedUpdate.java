/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;

import com.google.common.collect.Lists;

/**
 * The SerializableParsedTupleQuery class is an intermediate structure holding main parts of a query or a subquery:
 * projection, WHERE clause, GROUP BY clause, ORDER BY clause, LIMIT element, HAVING clause, and BINDINGS clause. These
 * fields are extracted from the {@link ParsedTupleQuery} tree.
 *
 * @author Andriy Nikolov
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 */
class SerializableParsedUpdate extends AbstractSerializableParsedQuery {

//    public MultiProjection projection = null;
	public UpdateExpr updateExpr = null;

//    public Group groupBy = null;
//    public Order orderBy = null;
//    public Filter having = null;

	public SerializableParsedUpdate() {

	}

	/**
	 * Returns the names of the variables projected by this query (as strings).
	 *
	 * @return list of projected variable names
	 */
	public List<String> getProjectionResultVars() {
		List<String> res = Lists.newArrayList();

		if (updateExpr instanceof Modify) {
			Modify modify = (Modify) updateExpr;
			if (modify.getInsertExpr() != null) {
				res.addAll(modify.getInsertExpr().getBindingNames());
			}
			if (modify.getDeleteExpr() != null) {
				res.addAll(modify.getDeleteExpr().getBindingNames());
			}
		}
		return res;
	}

}
