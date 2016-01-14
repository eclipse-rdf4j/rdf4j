/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene4;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.RAMDirectory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailGeoSPARQLTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene4.LuceneIndex;
import org.junit.Ignore;
import org.junit.Test;

public class LuceneSailGeoSPARQLTest extends AbstractLuceneSailGeoSPARQLTest {

	private LuceneIndex index;

	@Override
	protected void configure(LuceneSail sail)
		throws IOException
	{
		index = new LuceneIndex(new RAMDirectory(), new StandardAnalyzer());
		sail.setLuceneIndex(index);
	}


	@Test
	@Ignore // JTS is required
	@Override
	public void testIntersectionQuery()
		throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{
		super.testIntersectionQuery();
	}


	@Test
	@Ignore // JTS is required
	@Override
	public void testComplexIntersectionQuery()
		throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{
		super.testComplexIntersectionQuery();
	}
}
