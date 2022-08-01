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
package org.eclipse.rdf4j.common.app.logging.base;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler to convert java.util.logging events to SLF4J logging events.
 *
 * @author Herko ter Horst
 */
public class LogConverterHandler extends Handler {

	/**
	 * Constructor
	 */
	public LogConverterHandler() {
		setLevel(Level.ALL);
	}

	@Override
	public void close() throws SecurityException {
		// do nothing
	}

	@Override
	public void flush() {
		// do nothing
	}

	@Override
	public void publish(LogRecord record) {
		Logger logger = LoggerFactory.getLogger(record.getLoggerName());

		int level = record.getLevel().intValue();
		String message = record.getMessage();
		Throwable thrown = record.getThrown();

		if (level >= Level.SEVERE.intValue()) {
			logger.error(message, thrown);
		} else if (level < Level.SEVERE.intValue() && level >= Level.WARNING.intValue()) {
			logger.warn(message, thrown);
		} else if (level < Level.WARNING.intValue() || level >= Level.CONFIG.intValue()) {
			logger.info(message, thrown);
		} else {
			logger.debug(message, thrown);
		}
	}
}
