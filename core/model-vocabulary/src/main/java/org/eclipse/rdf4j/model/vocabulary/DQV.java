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

package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Data Quality Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/vocab-dqv/">Data Quality Vocabulary</a>
 *
 * @author Bart Hanssens
 */
public class DQV {
	/**
	 * The DQV namespace: http://www.w3.org/ns/dqv#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/dqv#";

	/**
	 * Recommended prefix for the namespace: "dqv"
	 */
	public static final String PREFIX = "dqv";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** dqv:Category */
	public static final IRI CATEGORY;

	/** dqv:Dimension */
	public static final IRI DIMENSION;

	/** dqv:Metric */
	public static final IRI METRIC;

	/** dqv:QualityAnnotation */
	public static final IRI QUALITY_ANNOTATION;

	/** dqv:QualityCertificate */
	public static final IRI QUALITY_CERTIFICATE;

	/** dqv:QualityMeasurement */
	public static final IRI QUALITY_MEASUREMENT;

	/** dqv:QualityMeasurementDataset */
	public static final IRI QUALITY_MEASUREMENT_DATASET;

	/** dqv:QualityMetadata */
	public static final IRI QUALITY_METADATA;

	/** dqv:QualityPolicy */
	public static final IRI QUALITY_POLICY;

	/** dqv:UserQualityFeedback */
	public static final IRI USER_QUALITY_FEEDBACK;

	// Properties
	/** dqv:computedOn */
	public static final IRI COMPUTED_ON;

	/** dqv:d5a835a95fd5465883e829dd6504b59610 */
	public static final IRI D5A835A95FD5465883E829DD6504B59610;

	/** dqv:expectedDataType */
	public static final IRI EXPECTED_DATA_TYPE;

	/** dqv:hasQualityAnnotation */
	public static final IRI HAS_QUALITY_ANNOTATION;

	/** dqv:hasQualityMeasurement */
	public static final IRI HAS_QUALITY_MEASUREMENT;

	/** dqv:hasQualityMetadata */
	public static final IRI HAS_QUALITY_METADATA;

	/** dqv:inCategory */
	public static final IRI IN_CATEGORY;

	/** dqv:inDimension */
	public static final IRI IN_DIMENSION;

	/** dqv:isMeasurementOf */
	public static final IRI IS_MEASUREMENT_OF;

	/** dqv:value */
	public static final IRI VALUE;

	// Individuals
	/** dqv:precision */
	public static final IRI PRECISION;

	static {
		CATEGORY = Vocabularies.createIRI(NAMESPACE, "Category");
		DIMENSION = Vocabularies.createIRI(NAMESPACE, "Dimension");
		METRIC = Vocabularies.createIRI(NAMESPACE, "Metric");
		QUALITY_ANNOTATION = Vocabularies.createIRI(NAMESPACE, "QualityAnnotation");
		QUALITY_CERTIFICATE = Vocabularies.createIRI(NAMESPACE, "QualityCertificate");
		QUALITY_MEASUREMENT = Vocabularies.createIRI(NAMESPACE, "QualityMeasurement");
		QUALITY_MEASUREMENT_DATASET = Vocabularies.createIRI(NAMESPACE, "QualityMeasurementDataset");
		QUALITY_METADATA = Vocabularies.createIRI(NAMESPACE, "QualityMetadata");
		QUALITY_POLICY = Vocabularies.createIRI(NAMESPACE, "QualityPolicy");
		USER_QUALITY_FEEDBACK = Vocabularies.createIRI(NAMESPACE, "UserQualityFeedback");

		COMPUTED_ON = Vocabularies.createIRI(NAMESPACE, "computedOn");
		D5A835A95FD5465883E829DD6504B59610 = Vocabularies.createIRI(NAMESPACE, "d5a835a95fd5465883e829dd6504b59610");
		EXPECTED_DATA_TYPE = Vocabularies.createIRI(NAMESPACE, "expectedDataType");
		HAS_QUALITY_ANNOTATION = Vocabularies.createIRI(NAMESPACE, "hasQualityAnnotation");
		HAS_QUALITY_MEASUREMENT = Vocabularies.createIRI(NAMESPACE, "hasQualityMeasurement");
		HAS_QUALITY_METADATA = Vocabularies.createIRI(NAMESPACE, "hasQualityMetadata");
		IN_CATEGORY = Vocabularies.createIRI(NAMESPACE, "inCategory");
		IN_DIMENSION = Vocabularies.createIRI(NAMESPACE, "inDimension");
		IS_MEASUREMENT_OF = Vocabularies.createIRI(NAMESPACE, "isMeasurementOf");
		VALUE = Vocabularies.createIRI(NAMESPACE, "value");

		PRECISION = Vocabularies.createIRI(NAMESPACE, "precision");
	}
}
