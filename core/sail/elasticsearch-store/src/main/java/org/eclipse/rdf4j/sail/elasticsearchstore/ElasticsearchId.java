/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

/**
 * @author Håvard Mikkelsen Ottestad
 */
interface ElasticsearchId extends ExtensibleStatement {
	String getElasticsearchId();
}
