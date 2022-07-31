/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.ParseLocationListener;

/**
 * A simple implementation of {@link ParseLocationListener}.
 *
 * @author Peter Ansell
 */
public class SimpleParseLocationListener implements ParseLocationListener {

	private long lineNo = 0;

	private long columnNo = 0;

	@Override
	public void parseLocationUpdate(long lineNo, long columnNo) {
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/**
	 * @return The last reported line number, -1 if the line number was not available when the location was updated, or
	 *         0 if none have been reported.
	 */
	public long getLineNo() {
		return lineNo;
	}

	/**
	 * @return The last reported column number, -1 if the column number was not available when the location was updated,
	 *         or 0 if none have been reported.
	 */
	public long getColumnNo() {
		return columnNo;
	}
}
