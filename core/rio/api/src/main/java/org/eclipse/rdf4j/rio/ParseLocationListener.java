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
package org.eclipse.rdf4j.rio;

/**
 * A listener interface for listening to the parser's progress.
 */
public interface ParseLocationListener {

	/**
	 * Signals an update of a parser's progress, indicated by a line and column number. Both line and column number
	 * start with value 1 for the first line or column.
	 *
	 * @param lineNo   The line number, or -1 if none is available.
	 * @param columnNo The column number, or -1 if none is available.
	 */
	void parseLocationUpdate(long lineNo, long columnNo);

}
