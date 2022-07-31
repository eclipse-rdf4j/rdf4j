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
 * Constants for the W3C Data Catalog Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/vocab-dcat/">Data Catalog Vocabulary</a>
 * @see <a href="https://www.w3.org/TR/vocab-dcat-2/">Data Catalog Vocabulary version 2</a>
 *
 * @author Bart Hanssens
 */
public class DCAT {

	/**
	 * Recommended prefix for the Data Catalog Vocabulary namespace: "dcat"
	 */
	public static final String PREFIX = "dcat";

	/**
	 * The DCAT namespace: http://www.w3.org/ns/dcat#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/dcat#";

	/**
	 * An immutable {@link Namespace} constant that represents the Data Catalog Vocabulary namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** dcat:Catalog */
	public static final IRI CATALOG;

	/** dcat:CatalogRecord */
	public static final IRI CATALOG_RECORD;

	/** dcat:DataService */
	public static final IRI DATA_SERVICE;

	/** dcat:Dataset */
	public static final IRI DATASET;

	/** dcat:Distribution */
	public static final IRI DISTRIBUTION;

	/** dcat:Relationship */
	public static final IRI RELATIONSHIP;

	/** dcat:Resource */
	public static final IRI RESOURCE;

	/** dcat:Role */
	public static final IRI ROLE;

	// Properties
	/** dcat:accessService */
	public static final IRI ACCESS_SERVICE;

	/** dcat:accessURL */
	public static final IRI ACCESS_URL;

	/** dcat:bbox */
	public static final IRI BBOX;

	/** dcat:byteSize */
	public static final IRI BYTE_SIZE;

	/** dcat:centroid */
	public static final IRI CENTROID;

	/** dcat:compressFormat */
	public static final IRI COMPRESS_FORMAT;

	/** dcat:contactPoint */
	public static final IRI CONTACT_POINT;

	/** dcat:downloadURL */
	public static final IRI DOWNLOAD_URL;

	/** dcat:endDate */
	public static final IRI END_DATE;

	/** dcat:endpointDescription */
	public static final IRI ENDPOINT_DESCRIPTION;

	/** dcat:endpointURL */
	public static final IRI ENDPOINT_URL;

	/** dcat:hadRole */
	public static final IRI HAD_ROLE;

	/** dcat:catalog */
	public static final IRI HAS_CATALOG;

	/** dcat:dataset */
	public static final IRI HAS_DATASET;

	/** dcat:distribution */
	public static final IRI HAS_DISTRIBUTION;

	/** dcat:record */
	public static final IRI HAS_RECORD;

	/** dcat:service */
	public static final IRI HAS_SERVICE;

	/** dcat:keyword */
	public static final IRI KEYWORD;

	/** dcat:landingPage */
	public static final IRI LANDING_PAGE;

	/** dcat:mediaType */
	public static final IRI MEDIA_TYPE;

	/** dcat:packageFormat */
	public static final IRI PACKAGE_FORMAT;

	/** dcat:qualifiedRelation */
	public static final IRI QUALIFIED_RELATION;

	/** dcat:servesDataset */
	public static final IRI SERVES_DATASET;

	/** dcat:spatialResolutionInMeters */
	public static final IRI SPATIAL_RESOLUTION_IN_METERS;

	/** dcat:startDate */
	public static final IRI START_DATE;

	/** dcat:temporalResolution */
	public static final IRI TEMPORAL_RESOLUTION;

	/** dcat:theme */
	public static final IRI THEME;

	/** dcat:themeTaxonomy */
	public static final IRI THEME_TAXONOMY;

	static {

		CATALOG = Vocabularies.createIRI(NAMESPACE, "Catalog");
		CATALOG_RECORD = Vocabularies.createIRI(NAMESPACE, "CatalogRecord");
		DATA_SERVICE = Vocabularies.createIRI(NAMESPACE, "DataService");
		DATASET = Vocabularies.createIRI(NAMESPACE, "Dataset");
		DISTRIBUTION = Vocabularies.createIRI(NAMESPACE, "Distribution");
		RELATIONSHIP = Vocabularies.createIRI(NAMESPACE, "Relationship");
		RESOURCE = Vocabularies.createIRI(NAMESPACE, "Resource");
		ROLE = Vocabularies.createIRI(NAMESPACE, "Role");

		ACCESS_SERVICE = Vocabularies.createIRI(NAMESPACE, "accessService");
		ACCESS_URL = Vocabularies.createIRI(NAMESPACE, "accessURL");
		BBOX = Vocabularies.createIRI(NAMESPACE, "bbox");
		BYTE_SIZE = Vocabularies.createIRI(NAMESPACE, "byteSize");
		CENTROID = Vocabularies.createIRI(NAMESPACE, "centroid");
		COMPRESS_FORMAT = Vocabularies.createIRI(NAMESPACE, "compressFormat");
		CONTACT_POINT = Vocabularies.createIRI(NAMESPACE, "contactPoint");
		DOWNLOAD_URL = Vocabularies.createIRI(NAMESPACE, "downloadURL");
		END_DATE = Vocabularies.createIRI(NAMESPACE, "endDate");
		ENDPOINT_DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "endpointDescription");
		ENDPOINT_URL = Vocabularies.createIRI(NAMESPACE, "endpointURL");
		HAD_ROLE = Vocabularies.createIRI(NAMESPACE, "hadRole");
		HAS_CATALOG = Vocabularies.createIRI(NAMESPACE, "catalog");
		HAS_DATASET = Vocabularies.createIRI(NAMESPACE, "dataset");
		HAS_DISTRIBUTION = Vocabularies.createIRI(NAMESPACE, "distribution");
		HAS_RECORD = Vocabularies.createIRI(NAMESPACE, "record");
		HAS_SERVICE = Vocabularies.createIRI(NAMESPACE, "service");
		KEYWORD = Vocabularies.createIRI(NAMESPACE, "keyword");
		LANDING_PAGE = Vocabularies.createIRI(NAMESPACE, "landingPage");
		MEDIA_TYPE = Vocabularies.createIRI(NAMESPACE, "mediaType");
		PACKAGE_FORMAT = Vocabularies.createIRI(NAMESPACE, "packageFormat");
		QUALIFIED_RELATION = Vocabularies.createIRI(NAMESPACE, "qualifiedRelation");
		SERVES_DATASET = Vocabularies.createIRI(NAMESPACE, "servesDataset");
		SPATIAL_RESOLUTION_IN_METERS = Vocabularies.createIRI(NAMESPACE, "spatialResolutionInMeters");
		START_DATE = Vocabularies.createIRI(NAMESPACE, "startDate");
		TEMPORAL_RESOLUTION = Vocabularies.createIRI(NAMESPACE, "temporalResolution");
		THEME = Vocabularies.createIRI(NAMESPACE, "theme");
		THEME_TAXONOMY = Vocabularies.createIRI(NAMESPACE, "themeTaxonomy");
	}
}
