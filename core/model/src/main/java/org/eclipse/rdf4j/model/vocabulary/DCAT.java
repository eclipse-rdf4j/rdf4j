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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		CATALOG = factory.createIRI(NAMESPACE, "Catalog");
		CATALOG_RECORD = factory.createIRI(NAMESPACE, "CatalogRecord");
		DATA_SERVICE = factory.createIRI(NAMESPACE, "DataService");
		DATASET = factory.createIRI(NAMESPACE, "Dataset");
		DISTRIBUTION = factory.createIRI(NAMESPACE, "Distribution");
		RELATIONSHIP = factory.createIRI(NAMESPACE, "Relationship");
		RESOURCE = factory.createIRI(NAMESPACE, "Resource");
		ROLE = factory.createIRI(NAMESPACE, "Role");

		ACCESS_SERVICE = factory.createIRI(NAMESPACE, "accessService");
		ACCESS_URL = factory.createIRI(NAMESPACE, "accessURL");
		BBOX = factory.createIRI(NAMESPACE, "bbox");
		BYTE_SIZE = factory.createIRI(NAMESPACE, "byteSize");
		CENTROID = factory.createIRI(NAMESPACE, "centroid");
		COMPRESS_FORMAT = factory.createIRI(NAMESPACE, "compressFormat");
		CONTACT_POINT = factory.createIRI(NAMESPACE, "contactPoint");
		DOWNLOAD_URL = factory.createIRI(NAMESPACE, "downloadURL");
		END_DATE = factory.createIRI(NAMESPACE, "endDate");
		ENDPOINT_DESCRIPTION = factory.createIRI(NAMESPACE, "endpointDescription");
		ENDPOINT_URL = factory.createIRI(NAMESPACE, "endpointURL");
		HAD_ROLE = factory.createIRI(NAMESPACE, "hadRole");
		HAS_CATALOG = factory.createIRI(NAMESPACE, "catalog");
		HAS_DATASET = factory.createIRI(NAMESPACE, "dataset");
		HAS_DISTRIBUTION = factory.createIRI(NAMESPACE, "distribution");
		HAS_RECORD = factory.createIRI(NAMESPACE, "record");
		HAS_SERVICE = factory.createIRI(NAMESPACE, "service");
		KEYWORD = factory.createIRI(NAMESPACE, "keyword");
		LANDING_PAGE = factory.createIRI(NAMESPACE, "landingPage");
		MEDIA_TYPE = factory.createIRI(NAMESPACE, "mediaType");
		PACKAGE_FORMAT = factory.createIRI(NAMESPACE, "packageFormat");
		QUALIFIED_RELATION = factory.createIRI(NAMESPACE, "qualifiedRelation");
		SERVES_DATASET = factory.createIRI(NAMESPACE, "servesDataset");
		SPATIAL_RESOLUTION_IN_METERS = factory.createIRI(NAMESPACE, "spatialResolutionInMeters");
		START_DATE = factory.createIRI(NAMESPACE, "startDate");
		TEMPORAL_RESOLUTION = factory.createIRI(NAMESPACE, "temporalResolution");
		THEME = factory.createIRI(NAMESPACE, "theme");
		THEME_TAXONOMY = factory.createIRI(NAMESPACE, "themeTaxonomy");
	}
}
