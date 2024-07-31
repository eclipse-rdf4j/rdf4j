/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;

/**
 * A class encapsulating writer settings that Turtle writers may support.
 *
 * @author Bart Hanssens
 *
 * @since 4.3.0
 */
public class TurtleWriterSettings {
	/**
	 * Boolean setting for Turtle/TriG Writer to determine if the abbreviated syntax for numeric datatypes is to be used
	 * when {@link org.eclipse.rdf4j.rio.helpers.BasicWriterSettings.html#PRETTY_PRINT} is <code>true</code>.
	 * <p>
	 * This setting has no effect when pretty print is false.
	 *
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.turtle.abbreviate_numbers}
	 *
	 * @see <a href="https://www.w3.org/TR/turtle/#abbrev">https://www.w3.org/TR/turtle/#abbrev</a>
	 * @since 3.7.0
	 */
	public static final RioSetting<Boolean> ABBREVIATE_NUMBERS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.turtle.abbreviate_numbers", "Abbreviate numbers", Boolean.TRUE);

	/**
	 * Private default constructor.
	 */
	private TurtleWriterSettings() {
	}

	static {
		assert ABBREVIATE_NUMBERS.equals(org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings.ABBREVIATE_NUMBERS);
	}

}
