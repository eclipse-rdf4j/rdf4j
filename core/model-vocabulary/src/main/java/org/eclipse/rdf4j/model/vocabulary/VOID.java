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
 * Constants for the W3C Vocabulary of Interlinked Datasets.
 *
 * @see <a href="https://www.w3.org/TR/void/">Vocabulary of Interlinked Datasets</a>
 *
 * @author Bart Hanssens
 */
public class VOID {

	/**
	 * The VoID namespace: http://rdfs.org/ns/void#
	 */
	public static final String NAMESPACE = "http://rdfs.org/ns/void#";

	/**
	 * Recommended prefix for the VoID namespace: "void"
	 */
	public static final String PREFIX = "void";

	/**
	 * An immutable {@link Namespace} constant that represents the VoID namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** void:Dataset */
	public static final IRI DATASET;

	/** void:DatasetDescription */
	public static final IRI DATASET_DESCRIPTION;

	/** void:Linkset */
	public static final IRI LINKSET;

	/** void:TechnicalFeature */
	public static final IRI TECHNICAL_FEATURE;

	// Properties
	/** void:class */
	public static final IRI CLASS;

	/** void:classPartition */
	public static final IRI CLASS_PARTITION;

	/** void:classes */
	public static final IRI CLASSES;

	/** void:dataDump */
	public static final IRI DATA_DUMP;

	/** void:distinctObjects */
	public static final IRI DISTINCT_OBJECTS;

	/** void:distinctSubjects */
	public static final IRI DISTINCT_SUBJECTS;

	/** void:documents */
	public static final IRI DOCUMENTS;

	/** void:entities */
	public static final IRI ENTITIES;

	/** void:exampleResource */
	public static final IRI EXAMPLE_RESOURCE;

	/** void:feature */
	public static final IRI FEATURE;

	/** void:inDataset */
	public static final IRI IN_DATASET;

	/** void:linkPredicate */
	public static final IRI LINK_PREDICATE;

	/** void:objectsTarget */
	public static final IRI OBJECTS_TARGET;

	/** void:openSearchDescription */
	public static final IRI OPEN_SEARCH_DESCRIPTION;

	/** void:properties */
	public static final IRI PROPERTIES;

	/** void:property */
	public static final IRI PROPERTY;

	/** void:propertyPartition */
	public static final IRI PROPERTY_PARTITION;

	/** void:rootResource */
	public static final IRI ROOT_RESOURCE;

	/** void:sparqlEndpoint */
	public static final IRI SPARQL_ENDPOINT;

	/** void:subjectsTarget */
	public static final IRI SUBJECTS_TARGET;

	/** void:subset */
	public static final IRI SUBSET;

	/** void:target */
	public static final IRI TARGET;

	/** void:triples */
	public static final IRI TRIPLES;

	/** void:uriLookupEndpoint */
	public static final IRI URI_LOOKUP_ENDPOINT;

	/** void:uriRegexPattern */
	public static final IRI URI_REGEX_PATTERN;

	/** void:uriSpace */
	public static final IRI URI_SPACE;

	/** void:vocabulary */
	public static final IRI VOCABULARY;

	static {

		DATASET = Vocabularies.createIRI(NAMESPACE, "Dataset");
		DATASET_DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "DatasetDescription");
		LINKSET = Vocabularies.createIRI(NAMESPACE, "Linkset");
		TECHNICAL_FEATURE = Vocabularies.createIRI(NAMESPACE, "TechnicalFeature");

		CLASS = Vocabularies.createIRI(NAMESPACE, "class");
		CLASS_PARTITION = Vocabularies.createIRI(NAMESPACE, "classPartition");
		CLASSES = Vocabularies.createIRI(NAMESPACE, "classes");
		DATA_DUMP = Vocabularies.createIRI(NAMESPACE, "dataDump");
		DISTINCT_OBJECTS = Vocabularies.createIRI(NAMESPACE, "distinctObjects");
		DISTINCT_SUBJECTS = Vocabularies.createIRI(NAMESPACE, "distinctSubjects");
		DOCUMENTS = Vocabularies.createIRI(NAMESPACE, "documents");
		ENTITIES = Vocabularies.createIRI(NAMESPACE, "entities");
		EXAMPLE_RESOURCE = Vocabularies.createIRI(NAMESPACE, "exampleResource");
		FEATURE = Vocabularies.createIRI(NAMESPACE, "feature");
		IN_DATASET = Vocabularies.createIRI(NAMESPACE, "inDataset");
		LINK_PREDICATE = Vocabularies.createIRI(NAMESPACE, "linkPredicate");
		OBJECTS_TARGET = Vocabularies.createIRI(NAMESPACE, "objectsTarget");
		OPEN_SEARCH_DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "openSearchDescription");
		PROPERTIES = Vocabularies.createIRI(NAMESPACE, "properties");
		PROPERTY = Vocabularies.createIRI(NAMESPACE, "property");
		PROPERTY_PARTITION = Vocabularies.createIRI(NAMESPACE, "propertyPartition");
		ROOT_RESOURCE = Vocabularies.createIRI(NAMESPACE, "rootResource");
		SPARQL_ENDPOINT = Vocabularies.createIRI(NAMESPACE, "sparqlEndpoint");
		SUBJECTS_TARGET = Vocabularies.createIRI(NAMESPACE, "subjectsTarget");
		SUBSET = Vocabularies.createIRI(NAMESPACE, "subset");
		TARGET = Vocabularies.createIRI(NAMESPACE, "target");
		TRIPLES = Vocabularies.createIRI(NAMESPACE, "triples");
		URI_LOOKUP_ENDPOINT = Vocabularies.createIRI(NAMESPACE, "uriLookupEndpoint");
		URI_REGEX_PATTERN = Vocabularies.createIRI(NAMESPACE, "uriRegexPattern");
		URI_SPACE = Vocabularies.createIRI(NAMESPACE, "uriSpace");
		VOCABULARY = Vocabularies.createIRI(NAMESPACE, "vocabulary");
	}
}
