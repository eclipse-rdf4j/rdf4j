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
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Bart Hanssens
 */
public class ConvertTest extends AbstractCommandTest {

	private Convert cmd;
	private File from;

	@Before
	public void setup() throws IOException, RDF4JException {
		when(mockConsoleIO.askProceed("File exists, continue ?", false)).thenReturn(Boolean.TRUE);
		cmd = new Convert(mockConsoleIO, mockConsoleState, defaultSettings);

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
		cmd.execute("convert", from.getAbsolutePath(), json.getAbsolutePath());

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
	public final void testConvertWorkDir() throws IOException {
		setWorkingDir(cmd);

		File json = LOCATION.newFile("alien.jsonld");
		cmd.execute("convert", from.getName(), json.getName());

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

		cmd.execute("convert", wrong.toString(), json.toString());
		verify(mockConsoleIO).writeError(anyString());
		assertFalse(mockConsoleIO.wasErrorWritten());
	}

	@Test
	public final void testConvertInvalidFormat() throws IOException {
		File qyx = LOCATION.newFile("alien.qyx");
		cmd.execute("convert", from.toString(), qyx.toString());
		verify(mockConsoleIO).writeError("No RDF writer for " + qyx.toString());
	}
}
