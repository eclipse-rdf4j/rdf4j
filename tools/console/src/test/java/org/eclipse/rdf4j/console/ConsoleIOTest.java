/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.when;

public class ConsoleIOTest {
	@Rule
	public final TemporaryFolder LOCATION = new TemporaryFolder();

	private ConsoleIO io;

	@Before
	public void initConsoleObject() throws IOException {
		InputStream input = mock(InputStream.class);
		OutputStream out = mock(OutputStream.class);
		ConsoleState info = mock(ConsoleState.class);
		when(info.getDataDirectory()).thenReturn(LOCATION.getRoot());

		io = new ConsoleIO(input, out, info);
	}

	@Test
	public void shouldSetErrorWrittenWhenErrorsAreWritten() {
		io.writeError(null);
		assertThat(io.wasErrorWritten()).isTrue();
	}

	@Test
	public void shouldSetErroWrittenOnParserError() {
		io.writeParseError("", 0, 0, "");
		assertThat(io.wasErrorWritten()).isTrue();
	}

	@Test
	public void shouldSetErroWrittenOnWriteUnoppenedError() {
		io.writeUnopenedError();
		assertThat(io.wasErrorWritten()).isTrue();
	}
}
