/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailIndexedPropertiesTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.common.io.FileSystemUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ElasticsearchSailIndexedPropertiesTest extends AbstractLuceneSailIndexedPropertiesTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private Path testDir;

	@Override
	protected void configure(LuceneSail sail) {
		sail.setParameter(ElasticsearchIndex.INDEX_NAME_KEY, ElasticsearchTestUtils.getNextTestIndexName());
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_DIR_KEY, testDir.toAbsolutePath().toString());
	}

	@Override
	public void setUp()
		throws Exception
	{
		ElasticsearchTestUtils.TEST_SEMAPHORE.acquire();
		testDir = tempDir.newFolder("es-ip-test").toPath();
		super.setUp();
	}

	@Override
	public void tearDown()
		throws IOException, RepositoryException
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
}
