/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailSpinTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.common.io.FileSystemUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ElasticsearchSailSpinTest extends AbstractLuceneSailSpinTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private Path testDir;

	@Override
	public void setUp()
		throws Exception
	{
		ElasticsearchTestUtils.TEST_SEMAPHORE.acquire();
		testDir = tempDir.newFolder("es-ss-test").toPath();
		super.setUp();
	}

	@Override
	public void tearDown()
		throws RepositoryException, IOException
	{
		try {
			super.tearDown();
		}
		finally {
			try {
				FileSystemUtils.deleteRecursively(testDir.toFile());
			}
			finally {
				ElasticsearchTestUtils.TEST_SEMAPHORE.release();
			}
		}
	}

	@Override
	protected void configure(Properties parameters) {
		parameters.setProperty(ElasticsearchIndex.INDEX_NAME_KEY, ElasticsearchTestUtils.getNextTestIndexName());
		parameters.setProperty(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		parameters.setProperty(LuceneSail.LUCENE_DIR_KEY, testDir.toAbsolutePath().toString());
		parameters.setProperty(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "green");
		parameters.setProperty(ElasticsearchIndex.WAIT_FOR_NODES_KEY, ">=1");
	}
}
