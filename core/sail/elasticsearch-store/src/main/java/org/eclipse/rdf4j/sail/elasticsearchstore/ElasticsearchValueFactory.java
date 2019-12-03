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
import org.eclipse.rdf4j.model.impl.AbstractValueFactory;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchValueFactory extends AbstractValueFactory {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final ElasticsearchValueFactory sharedInstance = new ElasticsearchValueFactory();

	/**
	 * Provide a single shared instance of a SimpleValueFactory.
	 *
	 * @return a singleton instance of SimpleValueFactory.
	 */
	static ElasticsearchValueFactory getInstance() {
		return sharedInstance;
	}

	/**
	 * Hidden constructor to enforce singleton pattern.
	 */
	private ElasticsearchValueFactory() {
	}

	ElasticsearchStatement createStatement(String elasticsearchID, Resource subject, IRI predicate, Value object) {
		return new ElasticsearchStatement(elasticsearchID, subject, predicate, object);
	}

	ElasticsearchContextStatement createStatement(String elasticsearchID, Resource subject, IRI predicate, Value object,
			Resource context) {
		return new ElasticsearchContextStatement(elasticsearchID, subject, predicate, object, context);
	}
}
