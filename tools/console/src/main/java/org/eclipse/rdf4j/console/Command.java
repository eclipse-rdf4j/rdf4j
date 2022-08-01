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
package org.eclipse.rdf4j.console;

import java.io.IOException;

/**
 * Abstraction of console commands.
 *
 * @author Dale Visser
 */
public interface Command {
	/**
	 * Execute the given parameters.
	 *
	 * @param parameters parameters typed by user
	 * @throws IOException if a problem occurs reading or writing
	 */
	void execute(String... parameters) throws IOException;

	/**
	 * Return the names of the settings used.
	 *
	 * @return string array of settings
	 */
	String[] usesSettings();
}
