/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.impl.SailImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class AutomaticClosingOfUnclosedIterationsTest {

	private static MemoryAppender memoryAppender;

	@BeforeAll
	static void setup() {
		Logger logger = (Logger) LoggerFactory.getLogger(CleanerIteration.class);
		memoryAppender = new MemoryAppender();
		memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		logger.setLevel(Level.WARN);
		logger.addAppender(memoryAppender);
		memoryAppender.start();
	}

	@BeforeEach
	public void afterEach() {
		memoryAppender.reset();
	}

	@Test
	public void testCleaner() throws InterruptedException {
		String message = "Forced closing of unclosed iteration.";
		Level level = Level.WARN;

		SailImpl sail = new SailImpl();
		try (SailConnection connection = sail.getConnection()) {
			createUnclosedIteration(connection);

			while (!memoryAppender.contains(message, level)) {
				System.gc();
				Thread.sleep(1);
			}

		}

		assertTrue(memoryAppender.contains(message, level));

	}

	private void createUnclosedIteration(SailConnection connection) {
		CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null, null,
				null, true);
		assertNotNull(statements);

	}

	static class MemoryAppender extends ListAppender<ILoggingEvent> {
		public void reset() {
			this.list.clear();
		}

		public boolean contains(String string, Level level) {
			return this.list.stream()
					.anyMatch(event -> event.toString().contains(string)
							&& event.getLevel().equals(level));
		}

	}
}
