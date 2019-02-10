/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

/**
 * Defines constants for the ShaclSail schema which is used by {@link ShaclSailFactory}s to initialize
 * {@link ShaclSail}s.
 * 
 * @author Jeen Broekstra
 */
public class ShaclSailSchema {

	/** The ShaclSail schema namespace (<code>http://rdf4j.org/config/sail/shacl#</code>). */
	public static final String NAMESPACE = "http://rdf4j.org/config/sail/shacl#";

	/** <code>http://rdf4j.org/config/sail/shacl#parallelValidation</code> */
	public final static IRI PARALLEL_VALIDATION = create("parallelValidation");

	/** <code>http://rdf4j.org/config/sail/shacl#undefinedTargetValidatesAllSubjects</code> */
	public final static IRI UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS = create(
			"undefinedTargetValidatesAllSubjects");

	/** <code>http://rdf4j.org/config/sail/shacl#logValidationPlans</code> */
	public final static IRI LOG_VALIDATION_PLANS = create("logValidationPlans");

	/** <code>http://rdf4j.org/config/sail/shacl#logValidationViolations</code> */
	public final static IRI LOG_VALIDATION_VIOLATIONS = create("logValidationViolations");

	/** <code>http://rdf4j.org/config/sail/shacl#ignoreNoShapesLoadedException</code> */
	public final static IRI IGNORE_NO_SHAPES_LOADED_EXCEPTION = create("ignoreNoShapesLoadedException");

	/** <code>http://rdf4j.org/config/sail/shacl#validationEnabled</code> */
	public final static IRI VALIDATION_ENABLED = create("validationEnabled");

	/** <code>http://rdf4j.org/config/sail/shacl#cacheSelectNodes</code> */
	public final static IRI CACHE_SELECT_NODES = create("cacheSelectNodes");

	/** <code>http://rdf4j.org/config/sail/shacl#globalLogValidationExecution</code> */
	public final static IRI GLOBAL_LOG_VALIDATION_EXECUTION = create("globalLogValidationExecution");

	private static final IRI create(String localName) {
		return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
	}

}
