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
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A {@link RioSetting} with a {@link Boolean} value. The given default for the setting can be overridden by means of a
 * System property with a name equal to the setting key, and a string value of "true" or "false" (ignoring case).
 *
 * @author Jeen Broekstra
 */
public class BooleanRioSetting extends AbstractRioSetting<Boolean> {

	private static final long serialVersionUID = 2732349679294063815L;

	/**
	 * Creates a new boolean {@link RioSetting}.
	 *
	 * @param key          A unique key to use for this setting.
	 * @param description  A short human-readable description for this setting.
	 * @param defaultValue An immutable value specifying the default for this setting.
	 */
	public BooleanRioSetting(String key, String description, Boolean defaultValue) {
		super(key, description, defaultValue);
	}

	/**
	 * Converts a String to a Boolean
	 *
	 * @return a Boolean representing the supplied string value. Iff the string value is "true" (ignoring case), the
	 *         returned Boolean will be {@code true}, otherwise {@code false}.
	 */
	@Override
	public Boolean convert(String stringValue) {
		return Boolean.valueOf(stringValue);
	}

}
