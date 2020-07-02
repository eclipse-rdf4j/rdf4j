/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.solr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailGeoSPARQLTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.junit.Ignore;
import org.junit.Test;

public class SolrSailGeoSPARQLTest extends AbstractLuceneSailGeoSPARQLTest {

	private static final String DATA_DIR = "target/test-data";

	@Override
	protected void configure(LuceneSail sail) {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, SolrIndex.class.getName());
		sail.setParameter(SolrIndex.SERVER_KEY, "embedded:");
	}

	@Override
	protected void loadPolygons() {
		// do nothing - JTS is required
	}

	@Override
	protected void checkPolygons() {
		// do nothing - JTS is required
	}

	@Test
	@Ignore // JTS is required
	@Override
	public void testIntersectionQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		super.testIntersectionQuery();
	}

	@Test
	@Ignore // JTS is required
	@Override
	public void testComplexIntersectionQuery()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		super.testComplexIntersectionQuery();
	}

	@Override
	public void tearDown() throws IOException, RepositoryException {
		super.tearDown();
		FileUtils.deleteDirectory(new File(DATA_DIR));
	}
}
