/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the W3C Data Catalog Vocabulary.
 *
 * @author Bart Hanssens
 * @see <a href="https://www.w3.org/TR/vocab-dcat/">Data Catalog Vocabulary</a>
 * @see <a href="https://www.w3.org/TR/vocab-dcat-2/">Data Catalog Vocabulary version 2</a>
 * @see <a href="https://www.w3.org/TR/vocab-dcat-3/">Data Catalog Vocabulary version 3</a>
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

	/** dcat;DatasetSeries */
	public static final IRI DATASET_SERIES;

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

	/** dcat;first */
	public static final IRI FIRST;

	/** dcat:hadRole */
	public static final IRI HAD_ROLE;

	/** dcat:catalog */
	public static final IRI HAS_CATALOG;

	/** dcat:hasCurrentVersion */
	public static final IRI HAS_CURRENT_VERSION;

	/** dcat:dataset */
	public static final IRI HAS_DATASET;

	/** dcat:distribution */
	public static final IRI HAS_DISTRIBUTION;

	/** dcat:record */
	public static final IRI HAS_RECORD;

	/** dcat:service */
	public static final IRI HAS_SERVICE;

	/** dcat;inCatalog, inverse property, see section 7 of DCAT v3 */
	public static final IRI IN_CATALOG;

	/** dcat:inSeries */
	public static final IRI IN_SERIES;

	/** dcat:isDistributionOf, inverse property, see section 7 of DCAT v3 */
	public static final IRI IS_DISTRIBUTION_OF;

	/** dcat:isVersionOf, inverse property, see section 7 of DCAT v3 */
	public static final IRI IS_VERSION_OF;

	/** dcat:keyword */
	public static final IRI KEYWORD;

	/** dcat:landingPage */
	public static final IRI LANDING_PAGE;

	/** dcat:last */
	public static final IRI LAST;

	/** dcat:mediaType */
	public static final IRI MEDIA_TYPE;

	/** dcat:next, inverse property, see section 7 of DCAT v3 */
	public static final IRI NEXT;

	/** dcat:nextVersion, inverse property, see section 7 of DCAT v3 */
	public static final IRI NEXT_VERSION;

	/** dcat:packageFormat */
	public static final IRI PACKAGE_FORMAT;

	/** dcat:prev */
	public static final IRI PREV;

	/** dcat:previousVersion */
	public static final IRI PREVIOUS_VERSION;

	/** dcat:seriesMember, inverse property, see section 7 of DCAT v3 */
	public static final IRI SERIES_MEMBER;

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

	/** dcat:version */
	public static final IRI VERSION;

	static {

		CATALOG = Vocabularies.createIRI(NAMESPACE, "Catalog");
		CATALOG_RECORD = Vocabularies.createIRI(NAMESPACE, "CatalogRecord");
		DATA_SERVICE = Vocabularies.createIRI(NAMESPACE, "DataService");
		DATASET = Vocabularies.createIRI(NAMESPACE, "Dataset");
		DATASET_SERIES = Vocabularies.createIRI(NAMESPACE, "DatasetSeries");
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
		FIRST = Vocabularies.createIRI(NAMESPACE, "first");
		HAD_ROLE = Vocabularies.createIRI(NAMESPACE, "hadRole");
		HAS_CATALOG = Vocabularies.createIRI(NAMESPACE, "catalog");
		HAS_DATASET = Vocabularies.createIRI(NAMESPACE, "dataset");
		HAS_CURRENT_VERSION = Vocabularies.createIRI(NAMESPACE, "hasCurrentVersion");
		HAS_DISTRIBUTION = Vocabularies.createIRI(NAMESPACE, "distribution");
		HAS_RECORD = Vocabularies.createIRI(NAMESPACE, "record");
		HAS_SERVICE = Vocabularies.createIRI(NAMESPACE, "service");
		IN_CATALOG = Vocabularies.createIRI(NAMESPACE, "inCatalog");
		IN_SERIES = Vocabularies.createIRI(NAMESPACE, "inSeries");
		IS_DISTRIBUTION_OF = Vocabularies.createIRI(NAMESPACE, "isDistributionOf");
		IS_VERSION_OF = Vocabularies.createIRI(NAMESPACE, "isVersionOf");
		KEYWORD = Vocabularies.createIRI(NAMESPACE, "keyword");
		LANDING_PAGE = Vocabularies.createIRI(NAMESPACE, "landingPage");
		LAST = Vocabularies.createIRI(NAMESPACE, "last");
		MEDIA_TYPE = Vocabularies.createIRI(NAMESPACE, "mediaType");
		NEXT = Vocabularies.createIRI(NAMESPACE, "next");
		NEXT_VERSION = Vocabularies.createIRI(NAMESPACE, "nextVersion");
		PACKAGE_FORMAT = Vocabularies.createIRI(NAMESPACE, "packageFormat");
		PREV = Vocabularies.createIRI(NAMESPACE, "prev");
		PREVIOUS_VERSION = Vocabularies.createIRI(NAMESPACE, "previousVersion");
		QUALIFIED_RELATION = Vocabularies.createIRI(NAMESPACE, "qualifiedRelation");
		SERIES_MEMBER = Vocabularies.createIRI(NAMESPACE, "seriesMember");
		SERVES_DATASET = Vocabularies.createIRI(NAMESPACE, "servesDataset");
		SPATIAL_RESOLUTION_IN_METERS = Vocabularies.createIRI(NAMESPACE, "spatialResolutionInMeters");
		START_DATE = Vocabularies.createIRI(NAMESPACE, "startDate");
		TEMPORAL_RESOLUTION = Vocabularies.createIRI(NAMESPACE, "temporalResolution");
		THEME = Vocabularies.createIRI(NAMESPACE, "theme");
		THEME_TAXONOMY = Vocabularies.createIRI(NAMESPACE, "themeTaxonomy");
		VERSION = Vocabularies.createIRI(NAMESPACE, "version");
	}
}
