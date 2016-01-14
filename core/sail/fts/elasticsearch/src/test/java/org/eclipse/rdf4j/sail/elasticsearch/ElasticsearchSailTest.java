/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.elasticsearch.ElasticsearchIndex;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.common.io.FileSystemUtils;

public class ElasticsearchSailTest extends AbstractLuceneSailTest {

	private static final String DATA_DIR = "target/test-data";

	@Override
	protected void configure(LuceneSail sail) {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_DIR_KEY, DATA_DIR);
		sail.setParameter(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "green");
		sail.setParameter(ElasticsearchIndex.WAIT_FOR_NODES_KEY, ">=1");
	}

	@Override
	public void tearDown()
		throws IOException, RepositoryException
	{
		super.tearDown();
		FileSystemUtils.deleteRecursively(new File(DATA_DIR));
	}
}
