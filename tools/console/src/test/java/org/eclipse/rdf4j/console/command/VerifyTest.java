/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.rdf4j.RDF4JException;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;

import static org.mockito.Mockito.mock;

/**
 * Test verify command
 * 
 * @author Bart Hanssens
 */
public class VerifyTest extends AbstractCommandTest {
	private Verify cmd;
	private ConsoleIO io;

	@Rule
	public final TemporaryFolder LOCATION = new TemporaryFolder();

	@Before
	public void prepare() throws IOException, RDF4JException {
		InputStream input = mock(InputStream.class);
		OutputStream out = mock(OutputStream.class);
		ConsoleState info = mock(ConsoleState.class);
		io = new ConsoleIO(input, out, info);

		cmd = new Verify(io);
	}

	/**
	 * Copy Turtle file from resource to temp directory
	 * 
	 * @param str name of the resource file
	 * @return path to file in temp directory
	 * @throws IOException
	 */
	private String copyFromRes(String str) throws IOException {
		File f = LOCATION.newFile(str);
		Files.copy(this.getClass().getResourceAsStream("/verify/" + str), f.toPath(),
				StandardCopyOption.REPLACE_EXISTING);
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
		File report = LOCATION.newFile();
		cmd.execute("verify", copyFromRes("ok.ttl"), copyFromRes("shacl_invalid.ttl"), report.toString());
		assertTrue(io.wasErrorWritten());
		assertTrue(Files.size(report.toPath()) > 0);
	}

	@Test
	public final void testShaclValid() throws IOException {
		File report = LOCATION.newFile();
		cmd.execute("verify", copyFromRes("ok.ttl"), copyFromRes("shacl_valid.ttl"), report.toString());
		assertFalse(Files.size(report.toPath()) > 0);
		assertFalse(io.wasErrorWritten());
	}
}
