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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;

/**
 * Defines constants for the BaseSail schema.
 *
 * @deprecated use {@link CONFIG} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class BaseSailSchema {

	public static final String NAMESPACE = CONFIG.NAMESPACE;

	/**
	 * The (obsolete)BaseSail schema namespace (<var>http://www.openrdf.org/config/sail/base#</var>).
	 */
	public static final String NAMESPACE_OBSOLETE = "http://www.openrdf.org/config/sail/base#";

	public final static IRI EVALUATION_STRATEGY_FACTORY = CONFIG.evaluationStrategyFactory;
	public final static IRI DEFAULT_QUERY_EVALUATION_MODE = CONFIG.defaultQueryEvaluationMode;

}
