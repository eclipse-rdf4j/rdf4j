/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.serql;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.queryrender.QueryRenderer;

/**
 * <p>
 * Implementation of the {@link QueryRenderer} interface which renders
 * {@link org.eclipse.rdf4j.query.parser.ParsedQuery} objects as strings in SeRQL
 * syntax
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public class SeRQLQueryRenderer implements QueryRenderer {

	public final static boolean SERQL_ONE_X_COMPATIBILITY_MODE = false;

	/**
	 * The renderer object
	 */
	private SerqlTupleExprRenderer mRenderer = new SerqlTupleExprRenderer();

	/**
	 * @inheritDoc
	 */
	public QueryLanguage getLanguage() {
		return QueryLanguage.SERQL;
	}

	/**
	 * @inheritDoc
	 */
	public String render(final ParsedQuery theQuery)
		throws Exception
	{
		mRenderer.reset();

		return mRenderer.render(theQuery.getTupleExpr());
	}
}
