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
	public static final IRI Asset;

	/** adms:AssetDistribution */
	public static final IRI AssetDistribution;

	/** adms:AssetRepository */
	public static final IRI AssetRepository;

	/** adms:Identifier */
	public static final IRI Identifier;

	// Properties
	/** adms:identifier */
	public static final IRI identifier;

	/** adms:includedAsset */
	public static final IRI includedAsset;

	/** adms:interoperabilityLevel */
	public static final IRI interoperabilityLevel;

	/** adms:last */
	public static final IRI last;

	/** adms:next */
	public static final IRI next;

	/** adms:prev */
	public static final IRI prev;

	/** adms:representationTechnique */
	public static final IRI representationTechnique;

	/** adms:sample */
	public static final IRI sample;

	/** adms:schemaAgency */
	public static final IRI schemaAgency;

	/** adms:schemeAgency */
	public static final IRI schemeAgency;

	/** adms:status */
	public static final IRI status;

	/** adms:supportedSchema */
	public static final IRI supportedSchema;

	/** adms:translation */
	public static final IRI translation;

	/** adms:versionNotes */
	public static final IRI versionNotes;

	static {
		Asset = Vocabularies.createIRI(NAMESPACE, "Asset");
		AssetDistribution = Vocabularies.createIRI(NAMESPACE, "AssetDistribution");
		AssetRepository = Vocabularies.createIRI(NAMESPACE, "AssetRepository");
		Identifier = Vocabularies.createIRI(NAMESPACE, "Identifier");

		identifier = Vocabularies.createIRI(NAMESPACE, "identifier");
		includedAsset = Vocabularies.createIRI(NAMESPACE, "includedAsset");
		interoperabilityLevel = Vocabularies.createIRI(NAMESPACE, "interoperabilityLevel");
		last = Vocabularies.createIRI(NAMESPACE, "last");
		next = Vocabularies.createIRI(NAMESPACE, "next");
		prev = Vocabularies.createIRI(NAMESPACE, "prev");
		representationTechnique = Vocabularies.createIRI(NAMESPACE, "representationTechnique");
		sample = Vocabularies.createIRI(NAMESPACE, "sample");
		schemaAgency = Vocabularies.createIRI(NAMESPACE, "schemaAgency");
		schemeAgency = Vocabularies.createIRI(NAMESPACE, "schemeAgency");
		status = Vocabularies.createIRI(NAMESPACE, "status");
		supportedSchema = Vocabularies.createIRI(NAMESPACE, "supportedSchema");
		translation = Vocabularies.createIRI(NAMESPACE, "translation");
		versionNotes = Vocabularies.createIRI(NAMESPACE, "versionNotes");
	}
}
