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

package org.eclipse.rdf4j.model.vocabulary.annotation;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

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
	public static final IRI Category;

	/** dqv:Dimension */
	public static final IRI Dimension;

	/** dqv:Metric */
	public static final IRI Metric;

	/** dqv:QualityAnnotation */
	public static final IRI QualityAnnotation;

	/** dqv:QualityCertificate */
	public static final IRI QualityCertificate;

	/** dqv:QualityMeasurement */
	public static final IRI QualityMeasurement;

	/** dqv:QualityMeasurementDataset */
	public static final IRI QualityMeasurementDataset;

	/** dqv:QualityMetadata */
	public static final IRI QualityMetadata;

	/** dqv:QualityPolicy */
	public static final IRI QualityPolicy;

	/** dqv:UserQualityFeedback */
	public static final IRI UserQualityFeedback;

	// Properties
	/** dqv:computedOn */
	public static final IRI computedOn;

	/** dqv:expectedDataType */
	public static final IRI expectedDataType;

	/** dqv:hasQualityAnnotation */
	public static final IRI hasQualityAnnotation;

	/** dqv:hasQualityMeasurement */
	public static final IRI hasQualityMeasurement;

	/** dqv:hasQualityMetadata */
	public static final IRI hasQualityMetadata;

	/** dqv:inCategory */
	public static final IRI inCategory;

	/** dqv:inDimension */
	public static final IRI inDimension;

	/** dqv:isMeasurementOf */
	public static final IRI isMeasurementOf;

	/** dqv:value */
	public static final IRI value;

	// Individuals
	/** dqv:precision */
	public static final IRI precision;

	static {
		Category = Vocabularies.createIRI(NAMESPACE, "Category");
		Dimension = Vocabularies.createIRI(NAMESPACE, "Dimension");
		Metric = Vocabularies.createIRI(NAMESPACE, "Metric");
		QualityAnnotation = Vocabularies.createIRI(NAMESPACE, "QualityAnnotation");
		QualityCertificate = Vocabularies.createIRI(NAMESPACE, "QualityCertificate");
		QualityMeasurement = Vocabularies.createIRI(NAMESPACE, "QualityMeasurement");
		QualityMeasurementDataset = Vocabularies.createIRI(NAMESPACE, "QualityMeasurementDataset");
		QualityMetadata = Vocabularies.createIRI(NAMESPACE, "QualityMetadata");
		QualityPolicy = Vocabularies.createIRI(NAMESPACE, "QualityPolicy");
		UserQualityFeedback = Vocabularies.createIRI(NAMESPACE, "UserQualityFeedback");

		computedOn = Vocabularies.createIRI(NAMESPACE, "computedOn");
		expectedDataType = Vocabularies.createIRI(NAMESPACE, "expectedDataType");
		hasQualityAnnotation = Vocabularies.createIRI(NAMESPACE, "hasQualityAnnotation");
		hasQualityMeasurement = Vocabularies.createIRI(NAMESPACE, "hasQualityMeasurement");
		hasQualityMetadata = Vocabularies.createIRI(NAMESPACE, "hasQualityMetadata");
		inCategory = Vocabularies.createIRI(NAMESPACE, "inCategory");
		inDimension = Vocabularies.createIRI(NAMESPACE, "inDimension");
		isMeasurementOf = Vocabularies.createIRI(NAMESPACE, "isMeasurementOf");
		value = Vocabularies.createIRI(NAMESPACE, "value");

		precision = Vocabularies.createIRI(NAMESPACE, "precision");
	}
}
