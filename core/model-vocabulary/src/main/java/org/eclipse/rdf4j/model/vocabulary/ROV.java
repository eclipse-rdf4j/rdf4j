/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the W3C Registered Organization Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/vocab-regorg/">Registered Organization Vocabulary</a>
 *
 * @author Bart Hanssens
 */
public class ROV {

	/**
	 * The ROV namespace: http://www.w3.org/ns/regorg#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/regorg#";

	/**
	 * Recommended prefix for the Registered Organization namespace: "rov"
	 */
	public static final String PREFIX = "rov";

	/**
	 * An immutable {@link Namespace} constant that represents the Registered Organization namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Class
	/**
	 * rov:RegisteredOrganization
	 */
	public static final IRI REGISTERED_ORGANIZATION;

	// Properties
	/**
	 * rov:hasRegisteredOrganization
	 */
	public static final IRI HAS_REGISTERED_ORGANIZATION;

	/**
	 * rov:legalName
	 */
	public static final IRI LEGAL_NAME;

	/**
	 * rov:orgActivity
	 */
	public static final IRI ORG_ACTIVITY;

	/**
	 * rov:orgStatus
	 */
	public static final IRI ORG_STATUS;

	/**
	 * rov:orgType
	 */
	public static final IRI ORG_TYPE;

	/**
	 * rov:registration
	 */
	public static final IRI REGISTRATION;

	static {

		REGISTERED_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "RegisteredOrganization");

		HAS_REGISTERED_ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "hasRegisteredOrganization");
		LEGAL_NAME = Vocabularies.createIRI(NAMESPACE, "legalName");
		ORG_ACTIVITY = Vocabularies.createIRI(NAMESPACE, "orgActivity");
		ORG_STATUS = Vocabularies.createIRI(NAMESPACE, "orgStatus");
		ORG_TYPE = Vocabularies.createIRI(NAMESPACE, "orgType");
		REGISTRATION = Vocabularies.createIRI(NAMESPACE, "registration");
	}
}
