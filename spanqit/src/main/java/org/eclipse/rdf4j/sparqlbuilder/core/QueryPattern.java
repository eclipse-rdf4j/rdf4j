/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriple;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.util.SpanqitUtils;

/**
 * A SPARQL Query Pattern (<code>WHERE</code> clause)
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GraphPattern">
 *      Query Pattern Definition</a>
 */
public class QueryPattern implements QueryElement {
	private static final String WHERE = "WHERE";

	private GraphPatternNotTriple where = GraphPatterns.and();

	QueryPattern() { }

	/**
	 * Add graph patterns to this query pattern. Adds the given patterns into
	 * this query pattern's group graph pattern
	 * 
	 * @param patterns
	 *            the patterns to add
	 * @return this
	 */
	public QueryPattern where(GraphPattern... patterns) {
		where.and(patterns);

		return this;
	}
	
	/**
	 * Set this query pattern's where clause
	 * @param where
	 * 		the {@link GraphPatternNotTriple} instance to set the where clause to
	 * @return
	 * 		this QueryPattern instance
	 */
	public QueryPattern where(GraphPatternNotTriple where) {
		this.where = GraphPatterns.and(where);
		
		return this;
	}
	
	@Override
	public String getQueryString() {
		StringBuilder whereClause = new StringBuilder();
		
		whereClause.append(WHERE).append(" ");
		if(where.hasQualifier()) {
			whereClause.append(SpanqitUtils.getBracedString(where.getQueryString()));
		} else {
			whereClause.append(where.getQueryString());
		}

		return whereClause.toString();
	}
}