/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.RDF4JException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Bart Hanssens
 */
public class ConvertTest extends AbstractCommandTest {

	private Convert convert;
	private File from;

	@Rule
	public final TemporaryFolder LOCATION = new TemporaryFolder();

	@Before
	public void prepare() throws IOException, RDF4JException {
		when(mockConsoleIO.askProceed("File exists, continue ?", false)).thenReturn(Boolean.TRUE);
		convert = new Convert(mockConsoleIO, mockConsoleState);

		from = LOCATION.newFile("alien.ttl");
		copyFromResource("convert/alien.ttl", from);
	}

	@After
	@Override
	public void tearDown() {
		from.delete();
	}

	@Test
	public final void testConvert() throws IOException {
		File json = LOCATION.newFile("alien.jsonld");
		convert.execute("convert", from.toString(), json.toString());

		assertTrue("File is empty", json.length() > 0);

		Object o = null;
		try {
			o = JsonUtils.fromInputStream(Files.newInputStream(json.toPath()));
		} catch (IOException ioe) {
			//
		}
		assertTrue("Invalid JSON", o != null);
	}

	@Test
	public final void testConvertParseError() throws IOException {
		File wrong = LOCATION.newFile("wrong.nt");
		Files.write(wrong.toPath(), "error".getBytes());
		File json = LOCATION.newFile("empty.jsonld");

		convert.execute("convert", wrong.toString(), json.toString());
		verify(mockConsoleIO).writeError(anyString());
	}

	@Test
	public final void testConvertInvalidFormat() throws IOException {
		File qyx = LOCATION.newFile("alien.qyx");
		convert.execute("convert", from.toString(), qyx.toString());
		verify(mockConsoleIO).writeError("No RDF writer for " + qyx.toString());
	}
}
