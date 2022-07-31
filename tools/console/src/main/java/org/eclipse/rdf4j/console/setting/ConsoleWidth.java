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
 * Console value setting
 *
 * @author Bart Hanssens
 */
public class ConsoleWidth extends ConsoleSetting<Integer> {
	public final static String NAME = "width";

	@Override
	public String getHelpLong() {
		return "set width=<number>             Set the width for query result tables\n";
	}

	/**
	 * Constructor
	 *
	 * Default width is 80.
	 */
	public ConsoleWidth() {
		super(80);
	}

	/**
	 * Constructor
	 *
	 * @param initValue
	 */
	public ConsoleWidth(Integer initValue) {
		super(initValue);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void set(Integer value) throws IllegalArgumentException {
		int val = value;

		if (val > 0) {
			super.set(val);
		} else {
			throw new IllegalArgumentException("Width must be a positive integer");
		}
	}

	@Override
	public void setFromString(String value) throws IllegalArgumentException {
		try {
			set(Integer.valueOf(value));
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException();
		}
	}
}
