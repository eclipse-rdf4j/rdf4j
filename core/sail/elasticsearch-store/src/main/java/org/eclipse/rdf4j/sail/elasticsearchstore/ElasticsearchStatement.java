/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleStatement;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementImpl;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchStatement extends ExtensibleStatementImpl implements ElasticsearchId {

	private String elasticsearchId;

	ElasticsearchStatement(String elasticsearchId, Resource subject, IRI predicate, Value object, boolean inferred) {
		super(subject, predicate, object, inferred);
		this.elasticsearchId = elasticsearchId;
	}

	public String getElasticsearchId() {
		return elasticsearchId;
	}
}
