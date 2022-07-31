/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Test log level setting
 *
 * @author Bart Hanssens
 */
public class LogLevelTest extends AbstractSettingTest {
	private Level originalLevel;

	@BeforeEach
	@Override
	public void setUp() {
		originalLevel = ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLevel();
		settings.put(LogLevel.NAME, new LogLevel());
		super.setUp();
	}

	@AfterEach
	public void tearDown() throws Exception {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(originalLevel);
	}

	@Test
	public void testNoValueShowsCurrentLevel() {
		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);

		setParameters.execute("set", "log");

		verify(mockConsoleIO).writeln("log: info");
		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testSettingLogChangesLevel() {
		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.DEBUG);

		setParameters.execute("set", "log=warning");

		assertEquals(Level.WARN, logger.getLevel());
	}

	@Test
	public void testSettingUnknownLevelIsLoggedAsError() {
		setParameters.execute("set", "log=chatty");

		verify(mockConsoleIO).writeError("unknown logging level: chatty");
		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testLevelsThatDoNotMatchSlf4jAreMapped() {
		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.WARN);

		setParameters.execute("set", "log");

		verify(mockConsoleIO).writeln("log: warning");
		verifyNoMoreInteractions(mockConsoleIO);
	}
}
