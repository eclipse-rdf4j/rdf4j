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

/**
 * A SPARQL Order By clause
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy"> SPARQL Order By Clause</a>
 */
public class OrderBy extends StandardQueryElementCollection<Orderable> {
	private static final String ORDER_BY = "ORDER BY";
	private static final String DELIMETER = " ";

	OrderBy() {
		super(ORDER_BY, DELIMETER, new ArrayList<>());
		printNameIfEmpty(false);
	}

	/**
	 * Add order conditions
	 *
	 * @param conditions the conditions to add
	 * @return this
	 */
	public OrderBy by(Orderable... conditions) {
		addElements(conditions);

		return this;
	}
}
