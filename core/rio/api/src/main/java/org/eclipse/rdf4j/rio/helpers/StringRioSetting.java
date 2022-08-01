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
 * A {@link RioSetting} with a {@link String} value. The given default for the setting can be overridden by means of a
 * system property with a name equal to the setting key.
 *
 * @author Jeen Broekstra
 */
public class StringRioSetting extends AbstractRioSetting<String> {

	private static final long serialVersionUID = -3723273606390299263L;

	public StringRioSetting(String key, String description, String defaultValue) {
		super(key, description, defaultValue);
	}

	@Override
	public String convert(String stringValue) {
		return stringValue;
	}

}
