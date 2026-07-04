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
 * A SPARQL Group By clause
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#groupby"> SPARQL Group By Clause</a>
 */
public class GroupBy extends StandardQueryElementCollection<Groupable> {
	private static final String GROUP_BY = "GROUP BY";
	private static final String DELIMETER = " ";

	GroupBy() {
		super(GROUP_BY, DELIMETER, new ArrayList<>());
		printNameIfEmpty(false);
	}

	/**
	 * Add group conditions
	 *
	 * @param groupables the group conditions
	 * @return this
	 */
	public GroupBy by(Groupable... groupables) {
		addElements(groupables);

		return this;
	}
}
