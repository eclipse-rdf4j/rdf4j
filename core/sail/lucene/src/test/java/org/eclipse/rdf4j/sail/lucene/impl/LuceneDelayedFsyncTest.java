/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test to verify that when fsync interval is set, the index uses DelayedSyncDirectoryWrapper. It also checks if the
 * index still works correctly while using the wrapper.
 *
 * @author Piotr Sowiński
 */
public class LuceneDelayedFsyncTest extends AbstractGenericLuceneTest {

	@TempDir
	public File dataDir;

	private LuceneIndex index;

	@Override
	protected void configure(LuceneSail sail) throws IOException {
		index = new LuceneIndex(new NIOFSDirectory(dataDir.toPath()), new StandardAnalyzer());
		var params = new Properties();
		params.setProperty(LuceneSail.FSYNC_INTERVAL_KEY, "5000"); // 5 seconds
		params.setProperty(LuceneSail.LUCENE_DIR_KEY, dataDir.getAbsolutePath());
		try {
			index.initialize(params);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		sail.setLuceneIndex(index);
	}

	@Test
	public void testIndexSettings() {
		assertNotNull(index);
		assertThat(index.getDirectory()).isInstanceOf(DelayedSyncDirectoryWrapper.class);
	}
}
