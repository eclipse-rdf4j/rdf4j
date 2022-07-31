/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

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

		CONTRIBUTOR = Vocabularies.createIRI(NAMESPACE, "contributor");
		COVERAGE = Vocabularies.createIRI(NAMESPACE, "coverage");
		CREATOR = Vocabularies.createIRI(NAMESPACE, "creator");
		DATE = Vocabularies.createIRI(NAMESPACE, "date");
		DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "description");
		FORMAT = Vocabularies.createIRI(NAMESPACE, "format");
		IDENTIFIER = Vocabularies.createIRI(NAMESPACE, "identifier");
		LANGUAGE = Vocabularies.createIRI(NAMESPACE, "language");
		PUBLISHER = Vocabularies.createIRI(NAMESPACE, "publisher");
		RELATION = Vocabularies.createIRI(NAMESPACE, "relation");
		RIGHTS = Vocabularies.createIRI(NAMESPACE, "rights");
		SOURCE = Vocabularies.createIRI(NAMESPACE, "source");
		SUBJECT = Vocabularies.createIRI(NAMESPACE, "subject");
		TITLE = Vocabularies.createIRI(NAMESPACE, "title");
		TYPE = Vocabularies.createIRI(NAMESPACE, "type");
	}
}
