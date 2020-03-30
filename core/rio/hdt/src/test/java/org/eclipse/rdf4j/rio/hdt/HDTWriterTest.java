/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.HDTWriterSettings;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Bart Hanssens
 */
public class HDTWriterTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private RDFWriter writer;

	@Before
	public void setUp() throws Exception {
		File f = folder.newFile();
		OutputStream os = Files.newOutputStream(f.toPath(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		writer = Rio.createWriter(RDFFormat.HDT, os);
	}

	@Test
	public void writeSimpleSPO() {
		// load original N-Triples file
		try (InputStream is = HDTWriterTest.class.getResourceAsStream("/test-orig.nt")) {
			RDFParser nt = Rio.createParser(RDFFormat.NTRIPLES);
			writer.getWriterConfig().set(HDTWriterSettings.ORIGINAL_FILE, "C:/test-orig.nt");
			nt.setRDFHandler(writer);
			nt.parse(is, "");
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// assertEquals("HDT file does not match original NT file", 0, f.size());
	}
}
