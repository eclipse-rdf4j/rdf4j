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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test verify command
 *
 * @author Bart Hanssens
 */
public class VerifyTest extends AbstractCommandTest {
	private Verify cmd;
	private ConsoleIO io;

	@BeforeEach
	public void setUp() throws IOException, RDF4JException {
		InputStream input = mock(InputStream.class);
		OutputStream out = mock(OutputStream.class);
		ConsoleState info = mock(ConsoleState.class);
		when(info.getDataDirectory()).thenReturn(locationFile);

		io = new ConsoleIO(input, out, info);

		cmd = new Verify(io, defaultSettings);
	}

	/**
	 * Copy Turtle file from resource to temp directory
	 *
	 * @param str name of the resource file
	 * @return path to file in temp directory
	 * @throws IOException
	 */
	private String copyFromRes(String str) throws IOException {
		File f = new File(locationFile, str);
		copyFromResource("verify/" + str, f);
		return f.getAbsolutePath();
	}

	@Test
	public final void testVerifyWrongFormat() {
		cmd.execute("verify", "does-not-exist.docx");
		assertTrue(io.wasErrorWritten());
	}

	@Test
	public final void testVerifyOK() throws IOException {
		cmd.execute("verify", copyFromRes("ok.ttl"));
		assertFalse(io.wasErrorWritten());
	}

	@Test
	public final void testVerifyOKWorkDir() throws IOException {
		setWorkingDir(cmd);

		copyFromRes("ok.ttl");

		cmd.execute("verify", "ok.ttl");
		assertFalse(io.wasErrorWritten());
	}

	@Test
	public final void testVerifyBrokenFile() throws IOException {
		cmd.execute("verify", copyFromRes("broken.ttl"));
		assertTrue(io.wasErrorWritten());
	}

	@Test
	public final void testVerifyMissingType() throws IOException {
		cmd.execute("verify", copyFromRes("missing_type.ttl"));
		assertTrue(io.wasErrorWritten());
	}

	@Test
	public final void testVerifySpaceIRI() throws IOException {
		cmd.execute("verify", copyFromRes("space_iri.ttl"));
		assertTrue(io.wasErrorWritten());
	}

	@Test
	public final void testVerifyWrongLang() throws IOException {
		cmd.execute("verify", copyFromRes("wrong_lang.ttl"));
		assertTrue(io.wasErrorWritten());
	}

	@Test
	public final void testShaclInvalid() throws IOException {
		File report = new File(locationFile, "testShaclInvalid");
		cmd.execute("verify", copyFromRes("ok.ttl"), copyFromRes("shacl_invalid.ttl"), report.toString());
		assertTrue(io.wasErrorWritten());
		assertTrue(Files.size(report.toPath()) > 0);
	}

	@Test
	public final void testShaclValid() throws IOException {
		File report = new File(locationFile, "testShaclValid");
		assertTrue(report.createNewFile());
		cmd.execute("verify", copyFromRes("ok.ttl"), copyFromRes("shacl_valid.ttl"), report.toString());

		verify(mockConsoleIO, never()).writeError(anyString());
		assertFalse(Files.size(report.toPath()) > 0);
		assertFalse(io.wasErrorWritten());
	}

	@Test
	public final void testShaclValidWorkDir() throws IOException {
		setWorkingDir(cmd);

		copyFromRes("ok.ttl");
		copyFromRes("shacl_valid.ttl");

		File report = new File(locationFile, "testShaclValidWorkDir");
		assertTrue(report.createNewFile());
		cmd.execute("verify", "ok.ttl", "shacl_valid.ttl", report.getName());

		verify(mockConsoleIO, never()).writeError(anyString());
		assertFalse(Files.size(report.toPath()) > 0);
		assertFalse(io.wasErrorWritten());
	}
}
