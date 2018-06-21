/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.eclipse.rdf4j.console.ConsoleParameters;

public class SetParametersTest extends AbstractCommandTest {

	@Mock
	ConsoleParameters consoleParameters;

	@InjectMocks
	SetParameters setParameters;

	private Level originalLevel;

	@Before
	public void setUp() {
		originalLevel = ((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLevel();
		
		// Start all tests assuming a base of Debug logging, then revert after the test
		((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);
	}

	@After
	public void tearDown()
		throws Exception
	{
		try {
			super.tearDown();
		}
		finally {
			((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(originalLevel);
		}
	}

	@Test
	public void testUnknownParametersAreErrors() {
		setParameters.execute("set", "unknown");

		verify(mockConsoleIO).writeError("unknown parameter: unknown");
		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testNoValueShowsCurrentLevel() {
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);

		setParameters.execute("set", "log");

		verify(mockConsoleIO).writeln("log: info");
		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testSettingLogChangesLevel() {
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
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
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.WARN);

		setParameters.execute("set", "log");

		verify(mockConsoleIO).writeln("log: warning");
		verifyNoMoreInteractions(mockConsoleIO);
	}

	@Test
	public void testShowWidth() {
		setParameters.execute("set", "width");
		// Mocks have int's setup to be 0, so not actually testing the real behaviour here which would be a positive value
		verify(mockConsoleIO).writeln("width: 0");
		verifyNoMoreInteractions(mockConsoleIO);
	}

}
