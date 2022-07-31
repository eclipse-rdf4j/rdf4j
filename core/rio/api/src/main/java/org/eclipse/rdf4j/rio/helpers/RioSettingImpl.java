/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
 * Basic implementation of {@link RioSetting} interface, without support for default override via system properties.
 *
 * @author Peter Ansell
 * @see StringRioSetting
 * @see BooleanRioSetting
 * @see LongRioSetting
 */
public final class RioSettingImpl<T> extends AbstractRioSetting<T> {

	private static final long serialVersionUID = 5522964700661094393L;

	public RioSettingImpl(String key, String description, T defaultValue) {
		super(key, description, defaultValue);
	}
}
