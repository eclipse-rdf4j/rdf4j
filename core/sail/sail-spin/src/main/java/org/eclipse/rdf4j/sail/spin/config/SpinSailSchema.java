/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary constants for RDF-based configuration of the SpinSail.
 * 
 * @author Jeen Broekstra
 */
public class SpinSailSchema {

	/**
	 * The SpinSail schema namespace ( <tt>http://www.openrdf.org/config/sail/spin#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/spin#";

	/**
	 * http://www.openrdf.org/config/sail/spin#axiomClosureNeeded
	 */
	public static final IRI AXIOM_CLOSURE_NEEDED = create("axiomClosureNeeded");

	private static final IRI create(String localName) {
		return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
	}

	private SpinSailSchema() {
	};
}
