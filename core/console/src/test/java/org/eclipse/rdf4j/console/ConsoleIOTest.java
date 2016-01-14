/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ConsoleIOTest {

	private ConsoleIO io;

	@Before
	public void initConsoleObject() {
		BufferedReader input = mock(BufferedReader.class);
		PrintStream out = mock(PrintStream.class);
		PrintStream err = mock(PrintStream.class);
		ConsoleState info = mock(ConsoleState.class);
		io = new ConsoleIO(input, out, err, info);
	}

	@Test
	public void shouldSetErrorWrittenWhenErrorsAreWritten() {
		io.writeError(null);
		assertThat(io.wasErrorWritten(), equalTo(true));
	}

	@Test
	public void shouldSetErroWrittenOnParserError() {
		io.writeParseError("", 0, 0, "");
		assertThat(io.wasErrorWritten(), equalTo(true));
	}

	@Test
	public void shouldSetErroWrittenOnWriteUnoppenedError() {
		io.writeUnopenedError();
		assertThat(io.wasErrorWritten(), equalTo(true));
	}
}
