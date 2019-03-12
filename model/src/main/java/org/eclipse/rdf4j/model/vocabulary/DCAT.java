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

	/** dcat:Dataset */
	public static final IRI DATASET;

	/** dcat:Distribution */
	public static final IRI DISTRIBUTION;

	// Properties
	/** dcat:accessURL */
	public static final IRI ACCESS_URL;

	/** dcat:byteSize */
	public static final IRI BYTE_SIZE;

	/** dcat:contactPoint */
	public static final IRI CONTACT_POINT;

	/** dcat:dataset */
	public static final IRI HAS_DATASET;

	/** dcat:distribution */
	public static final IRI HAS_DISTRIBUTION;

	/** dcat:downloadURL */
	public static final IRI DOWNLOAD_URL;

	/** dcat:keyword */
	public static final IRI KEYWORD;

	/** dcat:landingPage */
	public static final IRI LANDING_PAGE;

	/** dcat:mediaType */
	public static final IRI MEDIA_TYPE;

	/** dcat:record */
	public static final IRI HAS_RECORD;

	/** dcat:theme */
	public static final IRI THEME;

	/** dcat:themeTaxonomy */
	public static final IRI THEME_TAXONOMY;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		CATALOG = factory.createIRI(NAMESPACE, "Catalog");
		CATALOG_RECORD = factory.createIRI(NAMESPACE, "CatalogRecord");
		DATASET = factory.createIRI(NAMESPACE, "Dataset");
		DISTRIBUTION = factory.createIRI(NAMESPACE, "Distribution");

		ACCESS_URL = factory.createIRI(NAMESPACE, "accessURL");
		BYTE_SIZE = factory.createIRI(NAMESPACE, "byteSize");
		CONTACT_POINT = factory.createIRI(NAMESPACE, "contactPoint");
		HAS_DATASET = factory.createIRI(NAMESPACE, "dataset");
		HAS_DISTRIBUTION = factory.createIRI(NAMESPACE, "distribution");
		DOWNLOAD_URL = factory.createIRI(NAMESPACE, "downloadURL");
		KEYWORD = factory.createIRI(NAMESPACE, "keyword");
		LANDING_PAGE = factory.createIRI(NAMESPACE, "landingPage");
		MEDIA_TYPE = factory.createIRI(NAMESPACE, "mediaType");
		HAS_RECORD = factory.createIRI(NAMESPACE, "record");
		THEME = factory.createIRI(NAMESPACE, "theme");
		THEME_TAXONOMY = factory.createIRI(NAMESPACE, "themeTaxonomy");
	}
}
