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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleContextStatement;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementImpl;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchValueFactory extends SimpleValueFactory implements ExtensibleStatementHelper {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final ElasticsearchValueFactory sharedInstance = new ElasticsearchValueFactory();

	/**
	 * Provide a single shared instance of a SimpleValueFactory.
	 *
	 * @return a singleton instance of SimpleValueFactory.
	 */
	public static SimpleValueFactory getInstance() {
		return sharedInstance;
	}

	/**
	 * Hidden constructor to enforce singleton pattern.
	 */
	private ElasticsearchValueFactory() {
	}

	ExtensibleStatement createStatement(Resource subject, IRI predicate, Value object, boolean inferred) {
		return new ExtensibleStatementImpl(subject, predicate, object, inferred);
	}

	ExtensibleStatement createStatement(Resource subject, IRI predicate, Value object,
			Resource context, boolean inferred) {
		return new ExtensibleContextStatement(subject, predicate, object, context, inferred);
	}

	ElasticsearchStatement createStatement(String elasticsearchID, Resource subject, IRI predicate, Value object,
			boolean inferred) {
		return new ElasticsearchStatement(elasticsearchID, subject, predicate, object, inferred);
	}

	ElasticsearchContextStatement createStatement(String elasticsearchID, Resource subject, IRI predicate, Value object,
			Resource context, boolean inferred) {
		return new ElasticsearchContextStatement(elasticsearchID, subject, predicate, object, context, inferred);
	}

	@Override
	public ExtensibleStatement fromStatement(Statement statement, boolean inferred) {
		if (statement instanceof ElasticsearchId) {

			ElasticsearchId elasticsearchIdStatement = (ElasticsearchId) statement;

			if (elasticsearchIdStatement.isInferred() == inferred) {
				return elasticsearchIdStatement;
			}
		}

		return ExtensibleStatementHelper.getDefaultImpl().fromStatement(statement, inferred);

	}
}
