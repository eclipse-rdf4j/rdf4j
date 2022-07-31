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
package org.eclipse.rdf4j.console.setting;

/**
 * Save command history to a file.
 *
 * @author Bart Hanssens
 */
public class SaveHistory extends ConsoleSetting<Boolean> {
	public final static String NAME = "savehistory";

	@Override
	public String getHelpLong() {
		return "set saveHistory=<true|false>   Toggles saving of command history to a file\n";
	}

	/**
	 * Constructor
	 */
	public SaveHistory() {
		super(true);
	}

	/**
	 * Constructor
	 *
	 * @param initValue
	 */
	public SaveHistory(Boolean initValue) {
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
