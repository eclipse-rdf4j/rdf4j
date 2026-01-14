/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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
 * Test to verify that when transactional support is disabled, the Lucene index is configured with a periodic fsync. It
 * also checks if the index still works correctly in this mode.
 *
 * @author Piotr Sowiński
 */
public class LuceneNonTransactionalTest extends AbstractGenericLuceneTest {

	@TempDir
	public File dataDir;

	private LuceneIndex index;

	@Override
	protected void configure(LuceneSail sail) throws IOException {
		index = new LuceneIndex(new NIOFSDirectory(dataDir.toPath()), new StandardAnalyzer());
		var params = new Properties();
		params.setProperty(LuceneSail.TRANSACTIONAL_KEY, "false");
		params.setProperty(LuceneSail.FSYNC_INTERVAL_KEY, "100");
		params.setProperty(LuceneSail.LUCENE_DIR_KEY, dataDir.getAbsolutePath());
		try {
			index.initialize(params);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		sail.setLuceneIndex(index);
	}

	@Override
	protected long waitAfterCommitMillis() {
		return 200;
	}

	@Test
	public void testIndexSettings() {
		assertNotNull(index);
		// Make sure the thread for periodic fsync is running
		int matchingThreads = 0;
		int iteration = 0;
		while (matchingThreads == 0 && iteration++ < 5) {
			var allThreads = Thread.getAllStackTraces().keySet();
			matchingThreads = (int) allThreads.stream()
					.filter(t -> t.getName().startsWith("rdf4j-lucene-fsync-"))
					.count();
			if (matchingThreads == 0) {
				// Retry after a short wait to let the thread start
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		assertThat(matchingThreads).isGreaterThan(0);
	}
}
