/*
 * Copyright (C) 2019 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.util;

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

		public static final String NAMESPACE = "http://www.fluidops.com/config/fedx#";


		/*
		 * Properties
		 */

		public static final IRI SUPPORTS_ASK_QUERIES = vf.createIRI(NAMESPACE, "supportsASKQueries");

		public static final IRI REPOSITORY_LOCATION = vf.createIRI(NAMESPACE, "repositoryLocation");

		public static final IRI STORE = vf.createIRI(NAMESPACE, "store");

		public static final IRI REPOSITORY_SERVER = vf.createIRI(NAMESPACE, "repositoryServer");

		public static final IRI REPOSITORY_NAME = vf.createIRI(NAMESPACE, "repositoryName");
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
