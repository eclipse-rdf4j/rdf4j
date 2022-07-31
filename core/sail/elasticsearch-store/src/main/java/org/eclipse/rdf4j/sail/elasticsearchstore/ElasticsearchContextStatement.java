/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleContextStatement;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchContextStatement extends ExtensibleContextStatement implements ElasticsearchId {

	private final String elasticsearchId;

	ElasticsearchContextStatement(String elasticsearchId, Resource subject, IRI predicate, Value object,
			Resource context, boolean inferred) {
		super(subject, predicate, object, context, inferred);
		this.elasticsearchId = elasticsearchId;
	}

	@Override
	public String getElasticsearchId() {
		return elasticsearchId;
	}
}
