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

import org.eclipse.rdf4j.query.algebra.Projection;

class SerializableParsedBooleanQuery extends AbstractSerializableParsedQuery {

	// ASK queries do not have the root projection node
	// We consider them as SELECT * WHERE {...} LIMIT 1 queries
	public Projection projection = null;

	public SerializableParsedBooleanQuery() {

	}

}
