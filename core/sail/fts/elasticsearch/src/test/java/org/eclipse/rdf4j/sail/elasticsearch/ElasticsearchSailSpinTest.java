/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailSpinTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.common.io.FileSystemUtils;
import org.junit.After;

public class ElasticsearchSailSpinTest extends AbstractLuceneSailSpinTest {

	private static final String DATA_DIR = "target/test-data";

	@After
	public void tearDown()
		throws RepositoryException, IOException
	{
		super.tearDown();
		FileSystemUtils.deleteRecursively(new File(DATA_DIR));
	}

	@Override
	protected void configure(Properties parameters) {
		parameters.setProperty(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		parameters.setProperty(LuceneSail.LUCENE_DIR_KEY, DATA_DIR);
		parameters.setProperty(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "green");
		parameters.setProperty(ElasticsearchIndex.WAIT_FOR_NODES_KEY, ">=1");
	}
}
