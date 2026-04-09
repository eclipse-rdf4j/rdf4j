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
 * Constants for the HealthData.eu HealthDCAT-AP.
 *
 * @see <a href="https://healthdataeu.pages.code.europa.eu/healthdcat-ap/releases/release-6/">HealthData.eu
 *      HealthDCAT-AP</a>
 *
 * @author Bart Hanssens
 */
public class HEALTHDCATAP {
	/**
	 * The HEALTHDCATAP namespace: http://healthdataportal.eu/ns/health#
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
	public static final IRI ANALYTICS;

	/** healthdcatap:hasCodeValues */
	public static final IRI HAS_CODE_VALUES;

	/** healthdcatap:hasCodingSystem */
	public static final IRI HAS_CODING_SYSTEM;

	/** healthdcatap:hdab */
	public static final IRI HDAB;

	/** healthdcatap:healthCategory */
	public static final IRI HEALTH_CATEGORY;

	/** healthdcatap:healthTheme */
	public static final IRI HEALTH_THEME;

	/** healthdcatap:maxTypicalAge */
	public static final IRI MAX_TYPICAL_AGE;

	/** healthdcatap:minTypicalAge */
	public static final IRI MIN_TYPICAL_AGE;

	/** healthdcatap:numberOfRecords */
	public static final IRI NUMBER_OF_RECORDS;

	/** healthdcatap:numberOfUniqueIndividuals */
	public static final IRI NUMBER_OF_UNIQUE_INDIVIDUALS;

	/** healthdcatap:populationCoverage */
	public static final IRI POPULATION_COVERAGE;

	/** healthdcatap:retentionPeriod */
	public static final IRI RETENTION_PERIOD;

	static {

		ANALYTICS = Vocabularies.createIRI(NAMESPACE, "analytics");
		HAS_CODE_VALUES = Vocabularies.createIRI(NAMESPACE, "hasCodeValues");
		HAS_CODING_SYSTEM = Vocabularies.createIRI(NAMESPACE, "hasCodingSystem");
		HDAB = Vocabularies.createIRI(NAMESPACE, "hdab");
		HEALTH_CATEGORY = Vocabularies.createIRI(NAMESPACE, "healthCategory");
		HEALTH_THEME = Vocabularies.createIRI(NAMESPACE, "healthTheme");
		MAX_TYPICAL_AGE = Vocabularies.createIRI(NAMESPACE, "maxTypicalAge");
		MIN_TYPICAL_AGE = Vocabularies.createIRI(NAMESPACE, "minTypicalAge");
		NUMBER_OF_RECORDS = Vocabularies.createIRI(NAMESPACE, "numberOfRecords");
		NUMBER_OF_UNIQUE_INDIVIDUALS = Vocabularies.createIRI(NAMESPACE, "numberOfUniqueIndividuals");
		POPULATION_COVERAGE = Vocabularies.createIRI(NAMESPACE, "populationCoverage");
		RETENTION_PERIOD = Vocabularies.createIRI(NAMESPACE, "retentionPeriod");

	}
}
