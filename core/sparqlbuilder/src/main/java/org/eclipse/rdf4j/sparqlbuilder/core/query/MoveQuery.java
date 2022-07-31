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
 * A SPARQL MOVE Query
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#move"> SPARQL MOVE Query</a>
 */
public class MoveQuery extends DestinationSourceManagementQuery<MoveQuery> {
	private static final String MOVE = "MOVE";

	MoveQuery() {
	}

	@Override
	protected String getQueryActionString() {
		return MOVE;
	}

}
