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
package org.eclipse.rdf4j.sail.elasticsearchstore.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;

/**
 * Defines constants for the ElasticsearchStore schema which is used by {@link ElasticsearchStoreFactory}s to initialize
 * {@link ElasticsearchStore}s.
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class ElasticsearchStoreSchema {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	/** The ElasticsearchStore schema namespace (<code>http://rdf4j.org/config/sail/elasticsearchstore#</code>). */
	public static final String NAMESPACE = "http://rdf4j.org/config/sail/elasticsearchstore#";
	public static final String PREFIX = "ess";

	public final static IRI hostname = vf.createIRI(NAMESPACE, "hostname");
	public final static IRI port = vf.createIRI(NAMESPACE, "port");
	public final static IRI index = vf.createIRI(NAMESPACE, "index");
	public final static IRI clusterName = vf.createIRI(NAMESPACE, "clusterName");

}
