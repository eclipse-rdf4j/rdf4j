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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ZipUtilTest {

	@TempDir
	public File dir;

	@Test
	public void testWriteEntryNormal() throws IOException {
		File f = new File(dir, "testok.zip");
		f.createNewFile();

		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
			ZipEntry e = new ZipEntry("helloworld.txt");
			out.putNextEntry(e);
			out.write("hello world".getBytes());
			out.closeEntry();
		}

		ZipFile zf = new ZipFile(f);
		File subdir = new File(dir, "extract");
		subdir.mkdir();
		ZipUtil.extract(zf, subdir);

		assertTrue(new File(subdir, "helloworld.txt").exists(), () -> "File not extracted");
	}

	@Test
	public void testWriteEntryPathTraversing() throws IOException {
		File f = new File(dir, "testnotok.zip");
		f.createNewFile();

		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
			ZipEntry e = new ZipEntry("hello/../../world.txt");
			out.putNextEntry(e);
			out.write("hello world".getBytes());
			out.closeEntry();
		}

		ZipFile zf = new ZipFile(f);
		File subdir = new File(dir, "extract");
		subdir.mkdir();
		try {
			ZipUtil.extract(zf, subdir);
			fail("No exception thrown");
		} catch (IOException ioe) {
			assertTrue(ioe.getMessage().startsWith("Zip entry outside destination directory"));
		}
	}
}
