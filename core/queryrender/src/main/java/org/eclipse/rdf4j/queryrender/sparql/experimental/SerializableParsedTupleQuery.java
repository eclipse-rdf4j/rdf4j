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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;

/**
 * The SerializableParsedTupleQuery class is an intermediate structure holding main parts of a query or a subquery:
 * projection, WHERE clause, GROUP BY clause, ORDER BY clause, LIMIT element, HAVING clause, and BINDINGS clause. These
 * fields are extracted from the {@link ParsedTupleQuery} tree.
 *
 * @author Andriy Nikolov
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 */
class SerializableParsedTupleQuery extends AbstractSerializableParsedQuery {

	public enum QueryModifier {
		DISTINCT,
		REDUCED
	}

	public Projection projection = null;
	public Group groupBy = null;
	public Order orderBy = null;
	public Filter having = null;
	public QueryModifier modifier = null;

	public SerializableParsedTupleQuery() {
		super();
	}

	/**
	 * Returns the names of the variables projected by this query (as strings).
	 *
	 * @return list of projected variable names
	 */
	public List<String> getProjectionResultVars() {
		List<String> res = new ArrayList<>(
				projection.getProjectionElemList().getElements().size());

		for (ProjectionElem elem : projection.getProjectionElemList().getElements()) {
			res.add(elem.getProjectionAlias().orElse(elem.getName()));
		}

		return res;
	}

}
