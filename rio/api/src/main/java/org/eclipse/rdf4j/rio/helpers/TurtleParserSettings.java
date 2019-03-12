/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Parser Settings that are specific to {@link org.eclipse.rdf4j.rio.RDFFormat#TURTLE} parsers.
 * 
 * @author Peter Ansell
 */
public class TurtleParserSettings {

	/**
	 * Allows the Turtle parser to recognize <tt>@BASE</tt> and <tt>@PREFIX</tt> in a similar way to the SPARQL case
	 * insensitive directives.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.turtle.case_insensitive_directives}.
	 */
	public static final RioSetting<Boolean> CASE_INSENSITIVE_DIRECTIVES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.turtle.case_insensitive_directives",
			"Allows case-insensitive directives to be recognised", Boolean.FALSE);

}
