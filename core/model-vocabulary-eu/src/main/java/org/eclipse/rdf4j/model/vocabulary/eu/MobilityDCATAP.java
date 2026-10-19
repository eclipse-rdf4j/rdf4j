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
 * Constants for the NapCore Mobility DCAT-AP.
 *
 * @see <a href="https://mobilitydcat-ap.github.io/mobilityDCAT-AP/releases/1.1.0/">NapCore Mobility DCAT-AP</a>
 *
 * @author Bart Hanssens
 */
public class MobilityDCATAP {
	/**
	 * The Mobilityy DCAT-AP namespace: https://w3id.org/mobilitydcat-ap#
	 */
	public static final String NAMESPACE = "https://w3id.org/mobilitydcat-ap#";

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
	public static final IRI Assessment;

	/** mobilitydcatap:MobilityDataStandard */
	public static final IRI MobilityDataStandard;

	// Properties
	/** mobilitydcatap:applicationLayerProtocol */
	public static final IRI applicationLayerProtocol;

	/** mobilitydcatap:assessmentResult */
	public static final IRI assessmentResult;

	/** mobilitydcatap:communicationMethod */
	public static final IRI communicationMethod;

	/** mobilitydcatap:dataFormatNotes */
	public static final IRI dataFormatNotes;

	/** mobilitydcatap:georeferencingMethod */
	public static final IRI georeferencingMethod;

	/** mobilitydcatap:grammar */
	@Deprecated
	public static final IRI grammar;

	/** mobilitydcatap:intendedInformationService */
	public static final IRI intendedInformationService;

	/** mobilitydcatap:mobilityDataStandard */
	public static final IRI mobilityDataStandard;

	/** mobilitydcatap:mobilityTheme */
	public static final IRI mobilityTheme;

	/** mobilitydcatap:networkCoverage */
	public static final IRI networkCoverage;

	/** mobilitydcatap:schema */
	public static final IRI schema;

	/** mobilitydcatap:specificContentModel */
	public static final IRI specificContentModel;

	/** mobilitydcatap:transportMode */
	public static final IRI transportMode;

	static {
		Assessment = Vocabularies.createIRI(NAMESPACE, "Assessment");
		MobilityDataStandard = Vocabularies.createIRI(NAMESPACE, "MobilityDataStandard");

		applicationLayerProtocol = Vocabularies.createIRI(NAMESPACE, "applicationLayerProtocol");
		assessmentResult = Vocabularies.createIRI(NAMESPACE, "assessmentResult");
		communicationMethod = Vocabularies.createIRI(NAMESPACE, "communicationMethod");
		dataFormatNotes = Vocabularies.createIRI(NAMESPACE, "dataFormatNotes");
		georeferencingMethod = Vocabularies.createIRI(NAMESPACE, "georeferencingMethod");
		grammar = Vocabularies.createIRI(NAMESPACE, "grammar");
		intendedInformationService = Vocabularies.createIRI(NAMESPACE, "intendedInformationService");
		mobilityDataStandard = Vocabularies.createIRI(NAMESPACE, "mobilityDataStandard");
		mobilityTheme = Vocabularies.createIRI(NAMESPACE, "mobilityTheme");
		networkCoverage = Vocabularies.createIRI(NAMESPACE, "networkCoverage");
		schema = Vocabularies.createIRI(NAMESPACE, "schema");
		specificContentModel = Vocabularies.createIRI(NAMESPACE, "specificContentModel");
		transportMode = Vocabularies.createIRI(NAMESPACE, "transportMode");

	}
}
