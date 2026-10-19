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
 * Constants for the HealthData.eu HealthDCAT-AP.
 *
 * @see <a href="https://healthdataeu.pages.code.europa.eu/healthdcat-ap/releases/release-6/">HealthData.eu
 *      HealthDCAT-AP</a>
 *
 * @author Bart Hanssens
 */
public class HealthDCATAP {
	/**
	 * The Health DCAT-AP namespace: http://healthdataportal.eu/ns/health#
	 */
	public static final String NAMESPACE = "http://healthdataportal.eu/ns/health#";

	/**
	 * Recommended prefix for the namespace: "healthdcatap"
	 */
	public static final String PREFIX = "healthdcatap";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Properties

	/** healthdcatap:analytics */
	public static final IRI analytics;

	/** healthdcatap:hasCodeValues */
	public static final IRI hasCodeValues;

	/** healthdcatap:hasCodingSystem */
	public static final IRI hasCodingSystem;

	/** healthdcatap:hdab */
	public static final IRI hdab;

	/** healthdcatap:healthCategory */
	public static final IRI healthCategory;

	/** healthdcatap:healthTheme */
	public static final IRI healthTheme;

	/** healthdcatap:maxTypicalAge */
	public static final IRI maxTypicalAge;

	/** healthdcatap:minTypicalAge */
	public static final IRI minTypicalAge;

	/** healthdcatap:numberOfRecords */
	public static final IRI numberOfRecords;

	/** healthdcatap:numberOfUniqueIndividuals */
	public static final IRI numberOfUniqueIndividuals;

	/** healthdcatap:populationCoverage */
	public static final IRI populationCoverage;

	/** healthdcatap:retentionPeriod */
	public static final IRI retentionPeriod;

	static {

		analytics = Vocabularies.createIRI(NAMESPACE, "analytics");
		hasCodeValues = Vocabularies.createIRI(NAMESPACE, "hasCodeValues");
		hasCodingSystem = Vocabularies.createIRI(NAMESPACE, "hasCodingSystem");
		hdab = Vocabularies.createIRI(NAMESPACE, "hdab");
		healthCategory = Vocabularies.createIRI(NAMESPACE, "healthCategory");
		healthTheme = Vocabularies.createIRI(NAMESPACE, "healthTheme");
		maxTypicalAge = Vocabularies.createIRI(NAMESPACE, "maxTypicalAge");
		minTypicalAge = Vocabularies.createIRI(NAMESPACE, "minTypicalAge");
		numberOfRecords = Vocabularies.createIRI(NAMESPACE, "numberOfRecords");
		numberOfUniqueIndividuals = Vocabularies.createIRI(NAMESPACE, "numberOfUniqueIndividuals");
		populationCoverage = Vocabularies.createIRI(NAMESPACE, "populationCoverage");
		retentionPeriod = Vocabularies.createIRI(NAMESPACE, "retentionPeriod");
	}
}
