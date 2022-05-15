/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;

import com.google.common.collect.Lists;

/**
 * The SerializableParsedTupleQuery class is an intermediate structure holding main parts of a query or a subquery:
 * projection, WHERE clause, GROUP BY clause, ORDER BY clause, LIMIT element, HAVING clause, and BINDINGS clause. These
 * fields are extracted from the {@link ParsedTupleQuery} tree.
 */
class SerializableParsedConstructQuery extends AbstractSerializableParsedQuery {

	public MultiProjection projection = null;
	public Group groupBy = null;
	public Order orderBy = null;
	public Filter having = null;
	public boolean describe = false;

	public SerializableParsedConstructQuery() {

	}

	/**
	 * Returns the names of the variables projected by this query (as strings).
	 *
	 * @return list of projected variable names
	 */
	public List<String> getProjectionResultVars() {
		List<String> res = Lists.newArrayList();
		for (ProjectionElemList proj : projection.getProjections()) {
			for (ProjectionElem elem : proj.getElements()) {
				res.add(elem.getTargetName());
			}
		}
		return res;
	}

}
