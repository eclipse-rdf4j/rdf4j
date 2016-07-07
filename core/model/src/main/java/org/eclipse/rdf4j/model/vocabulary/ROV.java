/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		REGISTERED_ORGANIZATION = factory.createIRI(NAMESPACE, "RegisteredOrganization");

		HAS_REGISTERED_ORGANIZATION = factory.createIRI(NAMESPACE, "hasRegisteredOrganization");
		LEGAL_NAME = factory.createIRI(NAMESPACE, "legalName");
		ORG_ACTIVITY = factory.createIRI(NAMESPACE, "orgActivity");
		ORG_STATUS = factory.createIRI(NAMESPACE, "orgStatus");
		ORG_TYPE = factory.createIRI(NAMESPACE, "orgType");
		REGISTRATION = factory.createIRI(NAMESPACE, "registration");
	}
}
