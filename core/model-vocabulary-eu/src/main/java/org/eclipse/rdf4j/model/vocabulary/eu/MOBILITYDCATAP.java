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
 * Constants for the NapCore Mobility DCAT-AP.
 *
 * @see <a href="https://mobilitydcat-ap.github.io/mobilityDCAT-AP/releases/1.1.0/">NapCore Mobility DCAT-AP</a>
 *
 * @author Bart Hanssens
 */
public class MOBILITYDCATAP {
	/**
	 * The MOBILITYDCATAP namespace: http://w3id.org/mobilitydcat-ap#
	 */
	public static final String NAMESPACE = "http://w3id.org/mobilitydcat-ap#";

	/**
	 * Recommended prefix for the namespace: "mobilitydcatap"
	 */
	public static final String PREFIX = "mobilitydcatap";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** mobilitydcatap:Assessment */
	public static final IRI ASSESSMENT;

	/** mobilitydcatap:MobilityDataStandard */
	public static final IRI MOBILITY_DATA_STANDARD;

	// Properties
	/** mobilitydcatap:applicationLayerProtocol */
	public static final IRI APPLICATION_LAYER_PROTOCOL;

	/** mobilitydcatap:assessmentResult */
	public static final IRI ASSESSMENT_RESULT;

	/** mobilitydcatap:communicationMethod */
	public static final IRI COMMUNICATION_METHOD;

	/** mobilitydcatap:dataFormatNotes */
	public static final IRI DATA_FORMAT_NOTES;

	/** mobilitydcatap:georeferencingMethod */
	public static final IRI GEOREFERENCING_METHOD;

	/** mobilitydcatap:grammar */
	public static final IRI GRAMMAR;

	/** mobilitydcatap:intendedInformationService */
	public static final IRI INTENDED_INFORMATION_SERVICE;

	/** mobilitydcatap:mobilityDataStandard */
	public static final IRI MOBILITY_DATA_STANDARD_PROP;

	/** mobilitydcatap:mobilityTheme */
	public static final IRI MOBILITY_THEME;

	/** mobilitydcatap:networkCoverage */
	public static final IRI NETWORK_COVERAGE;

	/** mobilitydcatap:schema */
	public static final IRI SCHEMA;

	/** mobilitydcatap:transportMode */
	public static final IRI TRANSPORT_MODE;

	static {
		ASSESSMENT = Vocabularies.createIRI(NAMESPACE, "Assessment");
		MOBILITY_DATA_STANDARD = Vocabularies.createIRI(NAMESPACE, "MobilityDataStandard");

		APPLICATION_LAYER_PROTOCOL = Vocabularies.createIRI(NAMESPACE, "applicationLayerProtocol");
		ASSESSMENT_RESULT = Vocabularies.createIRI(NAMESPACE, "assessmentResult");
		COMMUNICATION_METHOD = Vocabularies.createIRI(NAMESPACE, "communicationMethod");
		DATA_FORMAT_NOTES = Vocabularies.createIRI(NAMESPACE, "dataFormatNotes");
		GEOREFERENCING_METHOD = Vocabularies.createIRI(NAMESPACE, "georeferencingMethod");
		GRAMMAR = Vocabularies.createIRI(NAMESPACE, "grammar");
		INTENDED_INFORMATION_SERVICE = Vocabularies.createIRI(NAMESPACE, "intendedInformationService");
		MOBILITY_DATA_STANDARD_PROP = Vocabularies.createIRI(NAMESPACE, "mobilityDataStandard");
		MOBILITY_THEME = Vocabularies.createIRI(NAMESPACE, "mobilityTheme");
		NETWORK_COVERAGE = Vocabularies.createIRI(NAMESPACE, "networkCoverage");
		SCHEMA = Vocabularies.createIRI(NAMESPACE, "schema");
		TRANSPORT_MODE = Vocabularies.createIRI(NAMESPACE, "transportMode");

	}
}
