/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

/**
 * Show prefixes when displaying results.
 *
 * @author Bart Hanssens
 */
public class ShowPrefix extends ConsoleSetting<Boolean> {
	public final static String NAME = "showprefix";

	@Override
	public String getHelpLong() {
		return "set showPrefix=<true|false>    Toggles use of prefixed names in query results\n";
	}

	/**
	 * Constructor
	 */
	public ShowPrefix() {
		super(true);

	}

	/**
	 * Constructor
	 *
	 * @param initValue
	 */
	public ShowPrefix(Boolean initValue) {
		super(initValue);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void setFromString(String value) throws IllegalArgumentException {
		set(Boolean.valueOf(value));
	}
}
