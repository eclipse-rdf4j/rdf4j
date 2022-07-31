/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author James Leigh
 */
public class TestNativeStoreUpgrade {

	private static final String ZIP_2_7_15 = "/nativerdf-2.7.15.zip";

	private static final String ZIP_2_7_15_INCONSISTENT = "/nativerdf-inconsistent-2.7.15.zip";

	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	@Test
	public void testDevel() throws IOException, SailException {
		File dataDir = tmpDir.getRoot();
		NativeStore store = new NativeStore(dataDir);
		try {
			store.init();
			try (NotifyingSailConnection con = store.getConnection()) {
				ValueFactory vf = store.getValueFactory();
				con.begin();
				con.addStatement(RDF.VALUE, RDFS.LABEL, vf.createLiteral("value"));
				con.commit();
			}
		} finally {
			store.shutDown();
		}
		new File(dataDir, "nativerdf.ver").delete();
		assertValue(dataDir);
		assertTrue(new File(dataDir, "nativerdf.ver").exists());
	}

	@Test
	public void test2715() throws IOException, SailException {
		File dataDir = tmpDir.getRoot();
		extractZipResource(ZIP_2_7_15, dataDir);
		assertFalse(new File(dataDir, "nativerdf.ver").exists());
		assertValue(dataDir);
		assertTrue(new File(dataDir, "nativerdf.ver").exists());
	}

	@Test
	public void test2715Inconsistent() throws IOException, SailException {
		File dataDir = tmpDir.getRoot();
		extractZipResource(ZIP_2_7_15_INCONSISTENT, dataDir);
		assertFalse(new File(dataDir, "nativerdf.ver").exists());
		NativeStore store = new NativeStore(dataDir);
		try {
			store.init();
			// we expect init to still succeed, but the store not to be marked as upgraded. See SES-2244.
			assertFalse(new File(dataDir, "nativerdf.ver").exists());
		} finally {
			store.shutDown();
		}

	}

	public void assertValue(File dataDir) throws SailException {
		NativeStore store = new NativeStore(dataDir);
		try {
			store.init();
			try (NotifyingSailConnection con = store.getConnection()) {
				ValueFactory vf = store.getValueFactory();
				CloseableIteration<? extends Statement, SailException> iter;
				iter = con.getStatements(RDF.VALUE, RDFS.LABEL, vf.createLiteral("value"), false);
				try {
					assertTrue(iter.hasNext());
				} finally {
					iter.close();
				}
			}
		} finally {
			store.shutDown();
		}
	}

	public void extractZipResource(String resource, File dir) throws IOException {
		try (InputStream in = TestNativeStoreUpgrade.class.getResourceAsStream(resource)) {
			ZipInputStream zip = new ZipInputStream(in);
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				File file = new File(dir, entry.getName());
				file.createNewFile();
				FileChannel ch = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.WRITE);
				ch.transferFrom(Channels.newChannel(zip), 0, entry.getSize());
				zip.closeEntry();
			}
		}
	}

}
