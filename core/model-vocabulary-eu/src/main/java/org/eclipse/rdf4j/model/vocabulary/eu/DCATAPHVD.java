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
 * Constants for the SEMIC DCAT-AP High Value Datasets.
 *
 * @see <a href="https://semiceu.github.io/DCAT-AP/releases/3.0.0-hvd/">DCAT-AP High Value Datasets</a>
 *
 * @author Bart Hanssens
 */
public class DCATAPHVD {
	/**
	 * The DCATAPHVD namespace: http://data.europa.eu/r5r/
	 */
	public static final String NAMESPACE = "http://data.europa.eu/r5r/";

	/**
	 * Recommended prefix for the namespace: "dcatap"
	 */
	public static final String PREFIX = "dcatap";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Properties
	/** dcatap:applicableLegislation */
	public static final IRI APPLICABLE_LEGISLATION;

	/** dcatap:hvdCategory */
	public static final IRI HVD_CATEGORY;

	static {

		APPLICABLE_LEGISLATION = Vocabularies.createIRI(NAMESPACE, "applicableLegislation");
		HVD_CATEGORY = Vocabularies.createIRI(NAMESPACE, "hvdCategory");
	}
}
