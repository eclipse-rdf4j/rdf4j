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
package org.eclipse.rdf4j.common.io;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ZipUtilTest {
	@Rule
	public TemporaryFolder dir = new TemporaryFolder();

	@Test
	public void testWriteEntryNormal() throws IOException {
		File f = dir.newFile("testok.zip");

		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
			ZipEntry e = new ZipEntry("helloworld.txt");
			out.putNextEntry(e);
			out.write("hello world".getBytes());
			out.closeEntry();
		}

		ZipFile zf = new ZipFile(f);
		File subdir = dir.newFolder("extract");
		ZipUtil.extract(zf, subdir);

		assertTrue("File not extracted", new File(subdir, "helloworld.txt").exists());
	}

	@Test
	public void testWriteEntryPathTraversing() throws IOException {
		File f = dir.newFile("testnotok.zip");

		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
			ZipEntry e = new ZipEntry("hello/../../world.txt");
			out.putNextEntry(e);
			out.write("hello world".getBytes());
			out.closeEntry();
		}

		ZipFile zf = new ZipFile(f);
		File subdir = dir.newFolder("extract");
		try {
			ZipUtil.extract(zf, subdir);
			fail("No exception thrown");
		} catch (IOException ioe) {
			assertTrue(ioe.getMessage().startsWith("Zip entry outside destination directory"));
		}
	}
}
