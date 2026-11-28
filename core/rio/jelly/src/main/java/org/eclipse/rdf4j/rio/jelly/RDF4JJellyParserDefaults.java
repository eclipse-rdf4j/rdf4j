/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jelly;

import eu.neverblink.jelly.convert.rdf4j.rio.JellyParserDefaults;
import eu.neverblink.jelly.convert.rdf4j.rio.JellyParserSettings;

/**
 * Makes the Jelly parser use RDF4J's checking parser stack by default, so that it respects the validation settings in
 * {@link org.eclipse.rdf4j.rio.helpers.BasicParserSettings} like RDF4J's other parsers do. Checking can still be turned
 * off per parser with {@link JellyParserSettings#CHECKING}, for faster parsing.
 */
public class RDF4JJellyParserDefaults implements JellyParserDefaults {

	@Override
	public boolean checking() {
		return true;
	}
}
