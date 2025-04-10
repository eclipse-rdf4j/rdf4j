/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

/**
 * Defines constants for the ShaclSail schema which is used by {@link ShaclSailFactory}s to initialize
 * {@link ShaclSail}s.
 *
 * @author Jeen Broekstra
 *
 * @deprecated since 4.3.0. Use {@link CONFIG.Shacl} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class ShaclSailSchema {

	/**
	 * The ShaclSail schema namespace (<code>http://rdf4j.org/config/sail/shacl#</code>).
	 */
	public static final String NAMESPACE = "http://rdf4j.org/config/sail/shacl#";

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#parallelValidation</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#parallelValidation} instead.
	 */
	public final static IRI PARALLEL_VALIDATION = create("parallelValidation");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#logValidationPlans</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#logValidationPlans} instead.
	 */
	public final static IRI LOG_VALIDATION_PLANS = create("logValidationPlans");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#logValidationViolations</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#logValidationViolations} instead.
	 */
	public final static IRI LOG_VALIDATION_VIOLATIONS = create("logValidationViolations");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#validationEnabled</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#validationEnabled} instead.
	 */
	public final static IRI VALIDATION_ENABLED = create("validationEnabled");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#cacheSelectNodes</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#cacheSelectNodes} instead.
	 */
	public final static IRI CACHE_SELECT_NODES = create("cacheSelectNodes");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#globalLogValidationExecution</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#globalLogValidationExecution} instead.
	 */
	public final static IRI GLOBAL_LOG_VALIDATION_EXECUTION = create("globalLogValidationExecution");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#rdfsSubClassReasoning</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#rdfsSubClassReasoning} instead.
	 */
	public final static IRI RDFS_SUB_CLASS_REASONING = create("rdfsSubClassReasoning");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#performanceLogging</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#performanceLogging} instead.
	 */
	public final static IRI PERFORMANCE_LOGGING = create("performanceLogging");

	/**
	 * <code>http://rdf4j.org/config/sail/shacl#serializableValidation</code>
	 *
	 * @deprecated use {@link CONFIG.Shacl#serializableValidation} instead.
	 */
	public final static IRI SERIALIZABLE_VALIDATION = create("serializableValidation");

	/**
	 * @deprecated use {@link CONFIG.Shacl#eclipseRdf4jShaclExtensions} instead.
	 */
	public final static IRI ECLIPSE_RDF4J_SHACL_EXTENSIONS = create("eclipseRdf4jShaclExtensions");

	/**
	 * @deprecated use {@link CONFIG.Shacl#dashDataShapes} instead.
	 */
	public final static IRI DASH_DATA_SHAPES = create("dashDataShapes");

	/**
	 * @deprecated use {@link CONFIG.Shacl#validationResultsLimitTotal} instead.
	 */
	public final static IRI VALIDATION_RESULTS_LIMIT_TOTAL = create("validationResultsLimitTotal");

	/**
	 * @deprecated use {@link CONFIG.Shacl#validationResultsLimitPerConstraint} instead.
	 */
	public final static IRI VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT = create("validationResultsLimitPerConstraint");

	/**
	 * @deprecated use {@link CONFIG.Shacl#transactionalValidationLimit} instead.
	 */
	public final static IRI TRANSACTIONAL_VALIDATION_LIMIT = create("transactionalValidationLimit");

	/**
	 * @deprecated use {@link CONFIG.Shacl#shapesGraph} instead.
	 */
	public final static IRI SHAPES_GRAPH = create("shapesGraph");

	private static IRI create(String localName) {
		return iri(NAMESPACE, localName);
	}

}
