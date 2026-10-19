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
import org.eclipse.rdf4j.model.base.Vocabularies;

/**
 * Constants for the SEMIC GeoDCAT-AP.
 *
 * @see <a href="https://semiceu.github.io/GeoDCAT-AP/930/releases/3.1.0/">SEMIC GeoDCAT-AP</a>
 *
 * @author Bart Hanssens
 */
public class GeoDCATAP {
	/**
	 * The GeoDCAT-AP namespace: http://data.europa.eu/930/
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

	// Properties
	/** geodcatap:custodian */
	public static final IRI custodian;

	/** geodcatap:distributor */
	public static final IRI distributor;

	/** geodcatap:originator */
	public static final IRI originator;

	/** geodcatap:principalInvestigator */
	public static final IRI principalInvestigator;

	/** geodcatap:processor */
	public static final IRI processor;

	/** geodcatap:purpose */
	public static final IRI purpose;

	/** geodcatap:referenceSystem */
	public static final IRI referenceSystem;

	/** geodcatap:resourceProvider */
	public static final IRI resourceProvider;

	/** geodcatap:resourceType */
	public static final IRI resourceType;

	/** geodcatap:serviceCategory */
	public static final IRI serviceCategory;

	/** geodcatap:serviceProtocol */
	public static final IRI serviceProtocol;

	/** geodcatap:serviceType */
	public static final IRI serviceType;

	/** geodcatap:spatialResolutionAsText */
	public static final IRI spatialResolutionAsText;

	/** geodcatap:topicCategory */
	public static final IRI topicCategory;

	/** geodcatap:user */
	public static final IRI user;

	static {
		custodian = Vocabularies.createIRI(NAMESPACE, "custodian");
		distributor = Vocabularies.createIRI(NAMESPACE, "distributor");
		originator = Vocabularies.createIRI(NAMESPACE, "originator");
		principalInvestigator = Vocabularies.createIRI(NAMESPACE, "principalInvestigator");
		processor = Vocabularies.createIRI(NAMESPACE, "processor");
		purpose = Vocabularies.createIRI(NAMESPACE, "purpose");
		referenceSystem = Vocabularies.createIRI(NAMESPACE, "referenceSystem");
		resourceProvider = Vocabularies.createIRI(NAMESPACE, "resourceProvider");
		resourceType = Vocabularies.createIRI(NAMESPACE, "resourceType");
		serviceCategory = Vocabularies.createIRI(NAMESPACE, "serviceCategory");
		serviceProtocol = Vocabularies.createIRI(NAMESPACE, "serviceProtocol");
		serviceType = Vocabularies.createIRI(NAMESPACE, "serviceType");
		spatialResolutionAsText = Vocabularies.createIRI(NAMESPACE, "spatialResolutionAsText");
		topicCategory = Vocabularies.createIRI(NAMESPACE, "topicCategory");
		user = Vocabularies.createIRI(NAMESPACE, "user");
	}
}
