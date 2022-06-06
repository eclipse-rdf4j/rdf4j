/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.util;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Utility class for generating query objects.
 */
public class QueryFactory {

	public static Query prepareQuery(final RepositoryConnection con, final QueryLanguage queryLn, final String query)
			throws RDF4JException {
		Query rval;
		try {
			rval = con.prepareQuery(queryLn, query);
		} catch (UnsupportedOperationException exc) {
			// TODO must be an HTTP repository
			try {
				con.prepareTupleQuery(queryLn, query).evaluate().close();
				rval = con.prepareTupleQuery(queryLn, query);
			} catch (Exception e1) {
				// guess its not a tuple query
				try {
					con.prepareGraphQuery(queryLn, query).evaluate().close();
					rval = con.prepareGraphQuery(queryLn, query);
				} catch (Exception e2) {
					// guess its not a graph query
					try {
						con.prepareBooleanQuery(queryLn, query).evaluate();
						rval = con.prepareBooleanQuery(queryLn, query);
					} catch (Exception e3) {
						// guess its not a boolean query
						// let's assume it is an malformed tuple query
						rval = con.prepareTupleQuery(queryLn, query);
					}
				}
			}
		}
		return rval;
	}
}
