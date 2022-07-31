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

package org.eclipse.rdf4j.sparqlbuilder.core.query;

/**
 * A SPARQL DROP Query
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#drop"> SPARQL DROP Query</a>
 */
public class DropQuery extends TargetedGraphManagementQuery<DropQuery> {
	private static final String DROP = "DROP";

	DropQuery() {
	}

	@Override
	protected String getQueryActionString() {
		return DROP;
	}
}
