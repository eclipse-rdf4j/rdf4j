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
package org.eclipse.rdf4j.federated.util;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary used in FedX to describe endpoints and services
 *
 * @author Andreas Schwarte
 *
 */
public class Vocabulary {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	/**
	 * FedX specific vocabulary
	 *
	 * @author Andreas Schwarte
	 *
	 */
	public static class FEDX {

		public static final String NAMESPACE = "http://rdf4j.org/config/federation#";

		/*
		 * Properties
		 */

		public static final IRI SUPPORTS_ASK_QUERIES = vf.createIRI(NAMESPACE, "supportsASKQueries");

		public static final IRI REPOSITORY_LOCATION = vf.createIRI(NAMESPACE, "repositoryLocation");

		public static final IRI STORE = vf.createIRI(NAMESPACE, "store");

		public static final IRI REPOSITORY_SERVER = vf.createIRI(NAMESPACE, "repositoryServer");

		public static final IRI REPOSITORY_NAME = vf.createIRI(NAMESPACE, "repositoryName");

		public static final IRI WRITABLE = vf.createIRI(NAMESPACE, "writable");
	}

	/**
	 * Vocabulary from the SPARQL 1.1. service description
	 *
	 * See https://www.w3.org/TR/sparql11-service-description/
	 *
	 * @author Andreas Schwarte
	 *
	 */
	public static class SD {

		public static final String NAMESPACE = "http://www.w3.org/ns/sparql-service-description#";

		public static final IRI SERVICE_TYPE = vf.createIRI(NAMESPACE, "Service");

		/*
		 * Properties
		 */

		public static final IRI ENDPOINT = vf.createIRI(NAMESPACE, "endpoint");
	}
}
