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
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.common.io.FileSystemUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchSailTest extends AbstractLuceneSailTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private Path testDir;

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	protected void configure(LuceneSail sail) {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_DIR_KEY, testDir.toAbsolutePath().toString());
		sail.setParameter(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "green");
		sail.setParameter(ElasticsearchIndex.WAIT_FOR_NODES_KEY, ">=1");
		log.debug("******* \t Data dir: {}", testDir.toAbsolutePath().toString());
	}

	@Override
	public void setUp()
		throws Exception
	{
		ElasticsearchTestUtils.TEST_SEMAPHORE.acquire();
		testDir = tempDir.newFolder("elasticsearch-sailspintest").toPath();
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

}
