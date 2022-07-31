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
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A {@link RioSetting} with a {@link Class} value. The given default for the setting can be overridden by means of a
 * system property with a name equal to the setting key.
 *
 * @author Bart Hanssens
 */
public class ClassRioSetting<T> extends AbstractRioSetting<T> {

	private static final long serialVersionUID = -4003273606390299263L;

	public ClassRioSetting(String key, String description, T defaultValue) {
		super(key, description, defaultValue);
	}

	@Override
	public T convert(String stringValue) {
		if (stringValue == null || stringValue.isEmpty()) {
			return null;
		}

		try {
			return (T) Class.forName(stringValue);
		} catch (ClassNotFoundException ex) {
			throw new RioConfigurationException(ex);
		}
	}
}
