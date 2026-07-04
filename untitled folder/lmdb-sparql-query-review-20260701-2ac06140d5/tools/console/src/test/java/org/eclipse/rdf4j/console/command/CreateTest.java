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
package org.eclipse.rdf4j.console.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test verify command
 *
 * @author Bart Hanssens
 */
public class CreateTest extends AbstractCommandTest {
	private Create cmd;
	private ConsoleIO io;

	@BeforeEach
	public void setUp() throws IOException, RDF4JException {
		InputStream input = mock(InputStream.class);
		OutputStream out = mock(OutputStream.class);
		when(mockConsoleState.getDataDirectory()).thenReturn(locationFile);
		when(mockConsoleState.getManager()).thenReturn(new LocalRepositoryManager(locationFile));

		io = new ConsoleIO(input, out, mockConsoleState) {

			@Override
			public String readMultiLineInput(String promt) {
				switch (promt) {
				default:
					return null;
				}
			}

			@Override
			public String readMultiLineInput() {
				return null;
			}

			@Override
			public String readln(String... message) {
				switch (message[0]) {
				case "Repository ID [memory]: ":
					return "Y";
				case "Repository title [Memory store]: ":
					return "Create-Test-Memory-Store";
				case "Query Iteration Cache sync threshold [10000]: ":
					return "10";
				case "Persist (true|false) [true]: ":
					return "false";
				case "Sync delay [0]: ":
					return "0";
				case "Query Evaluation Mode (STRICT|STANDARD) [STRICT]: ":
					return "STANDARD";
				}
				return null;
			}

			@Override
			public boolean askProceed(String msg, boolean defaultValue) {
				return true;
			}

		};

		cmd = new Create(io, mockConsoleState);
	}

	@Test
	public final void startCreate() {
		cmd.execute("create", "memory");
		assertFalse(io.wasErrorWritten());
	}

}
