/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary constants for the Dublin Core Metadata Element Set, version 1.1
 *
 * @author Jeen Broekstra
 * @see <a href="http://dublincore.org/documents/dces/">Dublin Core Metadata Element Set, Version 1.1</a>
 */
public class DC {

	/**
	 * Dublin Core elements namespace: http://purl.org/dc/elements/1.1/
	 */
	public static final String NAMESPACE = "http://purl.org/dc/elements/1.1/";

	/**
	 * Recommend prefix for the Dublin Core elements namespace: "dc"
	 */
	public static final String PREFIX = "dc";

	/**
	 * An immutable {@link Namespace} constant that represents the Dublin Core namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

	/**
	 * dc:title
	 */
	public static final IRI TITLE;

	/**
	 * dc:source
	 */
	public static final IRI SOURCE;

	/**
	 * dc:contributor
	 */
	public static final IRI CONTRIBUTOR;

	/**
	 * dc:coverage
	 */
	public static final IRI COVERAGE;

	/**
	 * dc:creator
	 */
	public static final IRI CREATOR;

	/**
	 * dc:date
	 */
	public static final IRI DATE;

	/**
	 * dc:description
	 */
	public static final IRI DESCRIPTION;

	/**
	 * dc:format
	 */
	public static final IRI FORMAT;

	/**
	 * dc:identifier
	 */
	public static final IRI IDENTIFIER;

	/**
	 * dc:language
	 */
	public static final IRI LANGUAGE;

	/**
	 * dc:publisher
	 */
	public static final IRI PUBLISHER;

	/**
	 * dc:relation
	 */
	public static final IRI RELATION;

	/**
	 * dc:rights
	 */
	public static final IRI RIGHTS;

	/**
	 * dc:subject
	 */
	public static final IRI SUBJECT;

	/**
	 * dc:type
	 */
	public static final IRI TYPE;

	static {

		CONTRIBUTOR = createIRI(NAMESPACE, "contributor");
		COVERAGE = createIRI(NAMESPACE, "coverage");
		CREATOR = createIRI(NAMESPACE, "creator");
		DATE = createIRI(NAMESPACE, "date");
		DESCRIPTION = createIRI(NAMESPACE, "description");
		FORMAT = createIRI(NAMESPACE, "format");
		IDENTIFIER = createIRI(NAMESPACE, "identifier");
		LANGUAGE = createIRI(NAMESPACE, "language");
		PUBLISHER = createIRI(NAMESPACE, "publisher");
		RELATION = createIRI(NAMESPACE, "relation");
		RIGHTS = createIRI(NAMESPACE, "rights");
		SOURCE = createIRI(NAMESPACE, "source");
		SUBJECT = createIRI(NAMESPACE, "subject");
		TITLE = createIRI(NAMESPACE, "title");
		TYPE = createIRI(NAMESPACE, "type");
	}
}
