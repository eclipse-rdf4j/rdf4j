/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A SPARQL Having clause
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#having"> SPARQL Having Clause</a>
 */
public class Having extends StandardQueryElementCollection<Expression<?>> {
	private static final String HAVING = "HAVING";
	private static final String DELIMETER = " ";

	Having() {
		super(HAVING, DELIMETER, SparqlBuilderUtils::getParenthesizedString, new ArrayList<>());
		printBodyIfEmpty(true);
	}

	/**
	 * Add having conditions
	 *
	 * @param expressions the conditions to add
	 * @return this
	 */
	public Having having(Expression<?>... expressions) {
		addElements(expressions);

		return this;
	}
}
