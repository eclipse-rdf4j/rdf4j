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

/**
 * An enumeration used to define constants used with the {@link BasicParserSettings#LARGE_LITERALS_HANDLING} parser
 * setting.
 *
 * @author Peter Ansell
 */
public enum LargeLiteralHandling {

	/**
	 * Indicates that large literals should be preserved. This is the default behaviour.
	 */
	PRESERVE,

	/**
	 * Indicates that statements containing large literals should be dropped, based on based on the
	 * {@link BasicParserSettings#LARGE_LITERALS_LIMIT} setting.
	 */
	DROP,

	/**
	 * Indicates that values of large literals should be truncated, based on the
	 * {@link BasicParserSettings#LARGE_LITERALS_LIMIT} setting.
	 */
	TRUNCATE

}
