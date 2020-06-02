/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A {@link RioSetting} with a {@link Long} value. The given default for the setting can be overriden by means of a
 * system property with a name equal to the setting key.
 *
 * @author Jeen Broekstra
 */
public class LongRioSetting extends AbstractRioSetting<Long> {

	private static final long serialVersionUID = 618659802892042423L;

	public LongRioSetting(String key, String description, Long defaultValue) {
		super(key, description, defaultValue);
	}

	@Override
	public Long convert(String stringValue) {
		try {
			return Long.parseLong(stringValue);
		} catch (NumberFormatException e) {
			throw new RioConfigurationException("Conversion error for setting: " + getKey(), e);
		}
	}

}
