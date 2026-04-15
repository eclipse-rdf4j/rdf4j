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
 * Constants for the SEMIC Asset Description Metadata Schema.
 *
 * @see <a href="https://semiceu.github.io/ADMS/releases/2.00/">SEMIC Asset Description Metadata Schema</a>
 *
 * @author Bart Hanssens
 */
public class ADMS {
	/**
	 * The ADMS namespace: http://www.w3.org/ns/adms#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/adms#";

	/**
	 * Recommended prefix for the namespace: "adms"
	 */
	public static final String PREFIX = "adms";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** adms:Asset */
	public static final IRI ASSET;

	/** adms:AssetDistribution */
	public static final IRI ASSET_DISTRIBUTION;

	/** adms:AssetRepository */
	public static final IRI ASSET_REPOSITORY;

	/** adms:Identifier */
	public static final IRI IDENTIFIER;

	// Properties
	/** adms:identifier */
	public static final IRI IDENTIFIER_PROP;

	/** adms:includedAsset */
	public static final IRI INCLUDED_ASSET;

	/** adms:interoperabilityLevel */
	public static final IRI INTEROPERABILITY_LEVEL;

	/** adms:last */
	public static final IRI LAST;

	/** adms:next */
	public static final IRI NEXT;

	/** adms:prev */
	public static final IRI PREV;

	/** adms:representationTechnique */
	public static final IRI REPRESENTATION_TECHNIQUE;

	/** adms:sample */
	public static final IRI SAMPLE;

	/** adms:schemaAgency */
	public static final IRI SCHEMA_AGENCY;

	/** adms:schemeAgency */
	public static final IRI SCHEME_AGENCY;

	/** adms:status */
	public static final IRI STATUS;

	/** adms:supportedSchema */
	public static final IRI SUPPORTED_SCHEMA;

	/** adms:translation */
	public static final IRI TRANSLATION;

	/** adms:versionNotes */
	public static final IRI VERSION_NOTES;

	static {
		ASSET = Vocabularies.createIRI(NAMESPACE, "Asset");
		ASSET_DISTRIBUTION = Vocabularies.createIRI(NAMESPACE, "AssetDistribution");
		ASSET_REPOSITORY = Vocabularies.createIRI(NAMESPACE, "AssetRepository");
		IDENTIFIER = Vocabularies.createIRI(NAMESPACE, "Identifier");

		IDENTIFIER_PROP = Vocabularies.createIRI(NAMESPACE, "identifier");
		INCLUDED_ASSET = Vocabularies.createIRI(NAMESPACE, "includedAsset");
		INTEROPERABILITY_LEVEL = Vocabularies.createIRI(NAMESPACE, "interoperabilityLevel");
		LAST = Vocabularies.createIRI(NAMESPACE, "last");
		NEXT = Vocabularies.createIRI(NAMESPACE, "next");
		PREV = Vocabularies.createIRI(NAMESPACE, "prev");
		REPRESENTATION_TECHNIQUE = Vocabularies.createIRI(NAMESPACE, "representationTechnique");
		SAMPLE = Vocabularies.createIRI(NAMESPACE, "sample");
		SCHEMA_AGENCY = Vocabularies.createIRI(NAMESPACE, "schemaAgency");
		SCHEME_AGENCY = Vocabularies.createIRI(NAMESPACE, "schemeAgency");
		STATUS = Vocabularies.createIRI(NAMESPACE, "status");
		SUPPORTED_SCHEMA = Vocabularies.createIRI(NAMESPACE, "supportedSchema");
		TRANSLATION = Vocabularies.createIRI(NAMESPACE, "translation");
		VERSION_NOTES = Vocabularies.createIRI(NAMESPACE, "versionNotes");

	}
}
