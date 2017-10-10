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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		DATASET = factory.createIRI(NAMESPACE, "Dataset");
		DATASET_DESCRIPTION = factory.createIRI(NAMESPACE, "DatasetDescription");
		LINKSET = factory.createIRI(NAMESPACE, "Linkset");
		TECHNICAL_FEATURE = factory.createIRI(NAMESPACE, "TechnicalFeature");

		CLASS = factory.createIRI(NAMESPACE, "class");
		CLASS_PARTITION = factory.createIRI(NAMESPACE, "classPartition");
		CLASSES = factory.createIRI(NAMESPACE, "classes");
		DATA_DUMP = factory.createIRI(NAMESPACE, "dataDump");
		DISTINCT_OBJECTS = factory.createIRI(NAMESPACE, "distinctObjects");
		DISTINCT_SUBJECTS = factory.createIRI(NAMESPACE, "distinctSubjects");
		DOCUMENTS = factory.createIRI(NAMESPACE, "documents");
		ENTITIES = factory.createIRI(NAMESPACE, "entities");
		EXAMPLE_RESOURCE = factory.createIRI(NAMESPACE, "exampleResource");
		FEATURE = factory.createIRI(NAMESPACE, "feature");
		IN_DATASET = factory.createIRI(NAMESPACE, "inDataset");
		LINK_PREDICATE = factory.createIRI(NAMESPACE, "linkPredicate");
		OBJECTS_TARGET = factory.createIRI(NAMESPACE, "objectsTarget");
		OPEN_SEARCH_DESCRIPTION = factory.createIRI(NAMESPACE, "openSearchDescription");
		PROPERTIES = factory.createIRI(NAMESPACE, "properties");
		PROPERTY = factory.createIRI(NAMESPACE, "property");
		PROPERTY_PARTITION = factory.createIRI(NAMESPACE, "propertyPartition");
		ROOT_RESOURCE = factory.createIRI(NAMESPACE, "rootResource");
		SPARQL_ENDPOINT = factory.createIRI(NAMESPACE, "sparqlEndpoint");
		SUBJECTS_TARGET = factory.createIRI(NAMESPACE, "subjectsTarget");
		SUBSET = factory.createIRI(NAMESPACE, "subset");
		TARGET = factory.createIRI(NAMESPACE, "target");
		TRIPLES = factory.createIRI(NAMESPACE, "triples");
		URI_LOOKUP_ENDPOINT = factory.createIRI(NAMESPACE, "uriLookupEndpoint");
		URI_REGEX_PATTERN = factory.createIRI(NAMESPACE, "uriRegexPattern");
		URI_SPACE = factory.createIRI(NAMESPACE, "uriSpace");
		VOCABULARY = factory.createIRI(NAMESPACE, "vocabulary");
	}
}
