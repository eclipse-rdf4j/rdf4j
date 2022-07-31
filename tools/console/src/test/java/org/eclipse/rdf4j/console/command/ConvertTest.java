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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jsonldjava.utils.JsonUtils;

/**
 * @author Bart Hanssens
 */
public class ConvertTest extends AbstractCommandTest {

	private Convert cmd;
	private File from;

	@BeforeEach
	public void setup() throws IOException, RDF4JException {
		when(mockConsoleIO.askProceed("File exists, continue ?", false)).thenReturn(Boolean.TRUE);
		cmd = new Convert(mockConsoleIO, mockConsoleState, defaultSettings);

		from = new File(locationFile, "alien.ttl");
		copyFromResource("convert/alien.ttl", from);
	}

	@AfterEach
	@Override
	public void tearDown() {
		from.delete();
	}

	@Test
	public final void testConvert() throws IOException {
		File json = new File(locationFile, "alien.jsonld");
		cmd.execute("convert", from.getAbsolutePath(), json.getAbsolutePath());

		assertTrue(json.length() > 0, "File is empty");

		Object o = null;
		try {
			o = JsonUtils.fromInputStream(Files.newInputStream(json.toPath()));
		} catch (IOException ioe) {
			//
		}
		assertTrue(o != null, "Invalid JSON");
	}

	@Test
	public final void testConvertWorkDir() throws IOException {
		setWorkingDir(cmd);

		File json = new File(locationFile, "alien.jsonld");
		cmd.execute("convert", from.getName(), json.getName());

		assertTrue(json.length() > 0, "File is empty");

		Object o = null;
		try {
			o = JsonUtils.fromInputStream(Files.newInputStream(json.toPath()));
		} catch (IOException ioe) {
			//
		}
		assertTrue(o != null, "Invalid JSON");
	}

	@Test
	public final void testConvertParseError() throws IOException {
		File wrong = new File(locationFile, "wrong.nt");
		Files.write(wrong.toPath(), "error".getBytes());
		File json = new File(locationFile, "empty.jsonld");

		cmd.execute("convert", wrong.toString(), json.toString());
		verify(mockConsoleIO).writeError(anyString());
		assertFalse(mockConsoleIO.wasErrorWritten());
	}

	@Test
	public final void testConvertInvalidFormat() throws IOException {
		File qyx = new File(locationFile, "alien.qyx");
		cmd.execute("convert", from.toString(), qyx.toString());
		verify(mockConsoleIO).writeError("No RDF writer for " + qyx.toString());
	}
}
