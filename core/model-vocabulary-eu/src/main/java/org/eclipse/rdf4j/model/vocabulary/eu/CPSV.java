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
import org.eclipse.rdf4j.model.base.Vocabularies;

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
	public static final IRI FormalFramework;

	/** cpsv:Input */
	public static final IRI Input;

	/** cpsv:Output */
	public static final IRI Output;

	/** cpsv:PublicService */
	public static final IRI PublicService;

	/** cpsv:Rule */
	public static final IRI Rule;

	// Properties
	/** cpsv:follows */
	public static final IRI follows;

	/** cpsv:hasInput */
	public static final IRI hasInput;

	/** cpsv:hasRole */
	public static final IRI hasRole;

	/** cpsv:implements */
	public static final IRI implements_;

	/** cpsv:physicallyAvailableAt */
	public static final IRI physicallyAvailableAt;

	/** cpsv:produces */
	public static final IRI produces;

	/** cpsv:provides */
	public static final IRI provides;

	/** cpsv:uses */
	public static final IRI uses;

	static {
		FormalFramework = Vocabularies.createIRI(NAMESPACE, "FormalFramework");
		Input = Vocabularies.createIRI(NAMESPACE, "Input");
		Output = Vocabularies.createIRI(NAMESPACE, "Output");
		PublicService = Vocabularies.createIRI(NAMESPACE, "PublicService");
		Rule = Vocabularies.createIRI(NAMESPACE, "Rule");

		follows = Vocabularies.createIRI(NAMESPACE, "follows");
		hasInput = Vocabularies.createIRI(NAMESPACE, "hasInput");
		hasRole = Vocabularies.createIRI(NAMESPACE, "hasRole");
		implements_ = Vocabularies.createIRI(NAMESPACE, "implements");
		physicallyAvailableAt = Vocabularies.createIRI(NAMESPACE, "physicallyAvailableAt");
		produces = Vocabularies.createIRI(NAMESPACE, "produces");
		provides = Vocabularies.createIRI(NAMESPACE, "provides");
		uses = Vocabularies.createIRI(NAMESPACE, "uses");
	}
}
