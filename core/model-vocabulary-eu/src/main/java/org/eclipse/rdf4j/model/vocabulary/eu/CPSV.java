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

package org.eclipse.rdf4j.model.vocabulary.eu;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the SEMIC Core Public Service Vocabulary.
 *
 * @see <a href="https://github.com/SEMICeu/CPSV">SEMIC Core Public Service Vocabulary</a>
 *
 * @author Bart Hanssens
 */
public class CPSV {
	/**
	 * The CPSV namespace: http://purl.org/vocab/cpsv#
	 */
	public static final String NAMESPACE = "http://purl.org/vocab/cpsv#";

	/**
	 * Recommended prefix for the namespace: "cpsv"
	 */
	public static final String PREFIX = "cpsv";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** cpsv:FormalFramework */
	public static final IRI FORMAL_FRAMEWORK;

	/** cpsv:Input */
	public static final IRI INPUT;

	/** cpsv:Output */
	public static final IRI OUTPUT;

	/** cpsv:PublicService */
	public static final IRI PUBLIC_SERVICE;

	/** cpsv:Rule */
	public static final IRI RULE;

	// Properties
	/** cpsv:follows */
	public static final IRI FOLLOWS;

	/** cpsv:hasInput */
	public static final IRI HAS_INPUT;

	/** cpsv:hasRole */
	public static final IRI HAS_ROLE;

	/** cpsv:implements */
	public static final IRI IMPLEMENTS;

	/** cpsv:physicallyAvailableAt */
	public static final IRI PHYSICALLY_AVAILABLE_AT;

	/** cpsv:produces */
	public static final IRI PRODUCES;

	/** cpsv:provides */
	public static final IRI PROVIDES;

	/** cpsv:uses */
	public static final IRI USES;

	static {
		FORMAL_FRAMEWORK = Vocabularies.createIRI(NAMESPACE, "FormalFramework");
		INPUT = Vocabularies.createIRI(NAMESPACE, "Input");
		OUTPUT = Vocabularies.createIRI(NAMESPACE, "Output");
		PUBLIC_SERVICE = Vocabularies.createIRI(NAMESPACE, "PublicService");
		RULE = Vocabularies.createIRI(NAMESPACE, "Rule");

		FOLLOWS = Vocabularies.createIRI(NAMESPACE, "follows");
		HAS_INPUT = Vocabularies.createIRI(NAMESPACE, "hasInput");
		HAS_ROLE = Vocabularies.createIRI(NAMESPACE, "hasRole");
		IMPLEMENTS = Vocabularies.createIRI(NAMESPACE, "implements");
		PHYSICALLY_AVAILABLE_AT = Vocabularies.createIRI(NAMESPACE, "physicallyAvailableAt");
		PRODUCES = Vocabularies.createIRI(NAMESPACE, "produces");
		PROVIDES = Vocabularies.createIRI(NAMESPACE, "provides");
		USES = Vocabularies.createIRI(NAMESPACE, "uses");

	}
}
