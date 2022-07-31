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

import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ParseErrorListener that reports Rio parser errors to the SLf4J Logging framework.
 *
 * @author jeen
 */
public class ParseErrorLogger implements ParseErrorListener {

	private final Logger logger = LoggerFactory.getLogger(ParseErrorLogger.class);

	@Override
	public void warning(String msg, long lineNo, long colNo) {
		logger.warn(msg + " (" + lineNo + ", " + colNo + ")");
	}

	@Override
	public void error(String msg, long lineNo, long colNo) {
		logger.warn("[Rio error] " + msg + " (" + lineNo + ", " + colNo + ")");
	}

	@Override
	public void fatalError(String msg, long lineNo, long colNo) {
		logger.error("[Rio fatal] " + msg + " (" + lineNo + ", " + colNo + ")");
	}

}
