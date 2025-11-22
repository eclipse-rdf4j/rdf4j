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
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.RAMDirectory;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.junit.jupiter.api.Test;

public class LuceneSailTest extends AbstractGenericLuceneTest {

	private LuceneIndex index;

	@Override
	protected void configure(LuceneSail sail) throws IOException {
		index = new LuceneIndex(new RAMDirectory(), new StandardAnalyzer());
		sail.setLuceneIndex(index);
	}

	@Test
	public void testIndexSettings() {
		assertNotNull(index);
		// Give the thread some time to start
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// Make sure the thread for periodic fsync is NOT running
		var allThreads = Thread.getAllStackTraces().keySet();
		assertThat(allThreads.stream().filter(t -> t.getName().startsWith("rdf4j-lucene-fsync-")).count())
				.isEqualTo(0);
	}
}
