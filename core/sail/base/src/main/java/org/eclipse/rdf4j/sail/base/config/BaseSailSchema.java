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
package org.eclipse.rdf4j.sail.base.config;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;

/**
 * Defines constants for the BaseSail schema.
 *
 * @deprecated use {@link CONFIG} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class BaseSailSchema {

	/**
	 * The (obsolete)BaseSail schema namespace (<var>http://www.openrdf.org/config/sail/base#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/base#";

	/**
	 * @deprecated use {@link CONFIG.Sail#evaluationStrategyFactory} instead.
	 */
	public final static IRI EVALUATION_STRATEGY_FACTORY = iri(NAMESPACE, "evaluationStrategyFactory");

	/**
	 * @deprecated use {@link CONFIG.Sail#defaultQueryEvaluationMode} instead.
	 */
	public final static IRI DEFAULT_QUERY_EVALUATION_MODE = iri(NAMESPACE, "defaultQueryEvaluationMode");

}
