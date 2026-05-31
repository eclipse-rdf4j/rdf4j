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

package org.eclipse.rdf4j.model.vocabulary.annotation;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

/**
 * Constants for the Dataset Usage Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/vocab-duv/">Dataset Usage Vocabulary</a>
 *
 * @author Bart Hanssens
 */
public class DUV {
	/**
	 * The DQV namespace: http://www.w3.org/ns/duv#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/duv#";

	/**
	 * Recommended prefix for the namespace: "duv"
	 */
	public static final String PREFIX = "duv";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** duv:RatingFeedback */
	public static final IRI RatingFeedback;

	/** duv:Usage */
	public static final IRI Usage;

	/** duv:UsageTool */
	public static final IRI UsageTool;

	/** duv:UserFeedback */
	public static final IRI UserFeedback;

	// Properties
	/** duv:hasDistributor */
	public static final IRI hasDistributor;

	/** duv:hasRating */
	public static final IRI hasRating;

	/** duv:hasUsage */
	public static final IRI hasUsage;

	/** duv:hasUsageToom */
	public static final IRI hasUsageTool;

	/** duv:performedBy */
	public static final IRI performedBy;

	/** duv:refersTo */
	public static final IRI refersTo;

	static {
		RatingFeedback = Vocabularies.createIRI(NAMESPACE, "RatingFeedback");
		Usage = Vocabularies.createIRI(NAMESPACE, "Usage");
		UsageTool = Vocabularies.createIRI(NAMESPACE, "UsageTool");
		UserFeedback = Vocabularies.createIRI(NAMESPACE, "UserFeedback");

		hasDistributor = Vocabularies.createIRI(NAMESPACE, "hasDistributor");
		hasRating = Vocabularies.createIRI(NAMESPACE, "hasRating");
		hasUsage = Vocabularies.createIRI(NAMESPACE, "hasUage");
		hasUsageTool = Vocabularies.createIRI(NAMESPACE, "hasUageTool");
		performedBy = Vocabularies.createIRI(NAMESPACE, "performedBy");
		refersTo = Vocabularies.createIRI(NAMESPACE, "refersTo");
	}
}
