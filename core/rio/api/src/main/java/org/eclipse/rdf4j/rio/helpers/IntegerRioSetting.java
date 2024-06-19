/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
 * A {@link RioSetting} with a {@link Integer} value. The given default for the setting can be overridden by means of a
 * system property with a name equal to the setting key.
 *
 * @author Håvard M. Ottestad
 */
public class IntegerRioSetting extends AbstractRioSetting<Integer> {

	private static final long serialVersionUID = -5945095126593465950L;

	public IntegerRioSetting(String key, String description, Integer defaultValue) {
		super(key, description, defaultValue);
	}

	@Override
	public Integer convert(String stringValue) {
		try {
			return Integer.parseInt(stringValue);
		} catch (NumberFormatException e) {
			throw new RioConfigurationException("Conversion error for setting: " + getKey(), e);
		}
	}

}
