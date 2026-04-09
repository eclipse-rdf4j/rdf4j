/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary.eu;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the SEMIC GeoDCAT-AP.
 *
 * @see <a href="https://semiceu.github.io/GeoDCAT-AP/930/releases/3.1.0/">SEMIC GeoDCAT-AP</a>
 *
 * @author Bart Hanssens
 */
public class GEODCATAP {
	/**
	 * The GEODCATAP namespace: http://data.europa.eu/930/
	 */
	public static final String NAMESPACE = "http://data.europa.eu/930/";

	/**
	 * Recommended prefix for the namespace: "geodcatap"
	 */
	public static final String PREFIX = "geodcatap";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes

	// Properties
	/** geodcatap:custodian */
	public static final IRI CUSTODIAN;

	/** geodcatap:distributor */
	public static final IRI DISTRIBUTOR;

	/** geodcatap:originator */
	public static final IRI ORIGINATOR;

	/** geodcatap:principalInvestigator */
	public static final IRI PRINCIPAL_INVESTIGATOR;

	/** geodcatap:processor */
	public static final IRI PROCESSOR;

	/** geodcatap:referenceSystem */
	public static final IRI REFERENCE_SYSTEM;

	/** geodcatap:resourceProvider */
	public static final IRI RESOURCE_PROVIDER;

	/** geodcatap:resourceType */
	public static final IRI RESOURCE_TYPE;

	/** geodcatap:serviceCategory */
	public static final IRI SERVICE_CATEGORY;

	/** geodcatap:serviceProtocol */
	public static final IRI SERVICE_PROTOCOL;

	/** geodcatap:serviceType */
	public static final IRI SERVICE_TYPE;

	/** geodcatap:spatialResolutionAsText */
	public static final IRI SPATIAL_RESOLUTION_AS_TEXT;

	/** geodcatap:topicCategory */
	public static final IRI TOPIC_CATEGORY;

	/** geodcatap:user */
	public static final IRI USER;

	static {

		CUSTODIAN = Vocabularies.createIRI(NAMESPACE, "custodian");
		DISTRIBUTOR = Vocabularies.createIRI(NAMESPACE, "distributor");
		ORIGINATOR = Vocabularies.createIRI(NAMESPACE, "originator");
		PRINCIPAL_INVESTIGATOR = Vocabularies.createIRI(NAMESPACE, "principalInvestigator");
		PROCESSOR = Vocabularies.createIRI(NAMESPACE, "processor");
		REFERENCE_SYSTEM = Vocabularies.createIRI(NAMESPACE, "referenceSystem");
		RESOURCE_PROVIDER = Vocabularies.createIRI(NAMESPACE, "resourceProvider");
		RESOURCE_TYPE = Vocabularies.createIRI(NAMESPACE, "resourceType");
		SERVICE_CATEGORY = Vocabularies.createIRI(NAMESPACE, "serviceCategory");
		SERVICE_PROTOCOL = Vocabularies.createIRI(NAMESPACE, "serviceProtocol");
		SERVICE_TYPE = Vocabularies.createIRI(NAMESPACE, "serviceType");
		SPATIAL_RESOLUTION_AS_TEXT = Vocabularies.createIRI(NAMESPACE, "spatialResolutionAsText");
		TOPIC_CATEGORY = Vocabularies.createIRI(NAMESPACE, "topicCategory");
		USER = Vocabularies.createIRI(NAMESPACE, "user");

	}
}
