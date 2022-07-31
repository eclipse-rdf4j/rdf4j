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
 * Use prefixes when querying.
 *
 * @author Bart Hanssens
 */
public class QueryPrefix extends ConsoleSetting<Boolean> {
	public final static String NAME = "queryprefix";

	@Override
	public String getHelpLong() {
		return "set queryPrefix=<true|false>   Toggles automatic use of known namespace prefixes in queries\n";
	}

	/**
	 * Constructor
	 */
	public QueryPrefix() {
		super(true);
	}

	/**
	 * Constructor
	 *
	 * @param initValue
	 */
	public QueryPrefix(Boolean initValue) {
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
