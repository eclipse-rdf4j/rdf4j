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
package org.eclipse.rdf4j.console;

/**
 * Help interface
 *
 * @author Bart Hanssens
 */
public interface Help {
	/**
	 * Get the name of the command
	 *
	 * @return string
	 */
	String getName();

	/**
	 * Get short description, small enough to fit on one console row
	 *
	 * @return string
	 */
	String getHelpShort();

	/**
	 * Get long description
	 *
	 * @return string, can be multiple lines
	 */
	String getHelpLong();
}
