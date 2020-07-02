/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@ClusterScope(numDataNodes = 1)
public class ElasticsearchSailTest extends ESIntegTestCase {

	AbstractLuceneSailTest delegateTest;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		TransportClient client = (TransportClient) internalCluster().transportClient();
		delegateTest = new AbstractLuceneSailTest() {

			@Override
			protected void configure(LuceneSail sail) {
				sail.setParameter(ElasticsearchIndex.TRANSPORT_KEY, client.transportAddresses().get(0).toString());
				sail.setParameter(ElasticsearchIndex.ELASTICSEARCH_KEY_PREFIX + "cluster.name",
						client.settings().get("cluster.name"));
				sail.setParameter(ElasticsearchIndex.INDEX_NAME_KEY, ElasticsearchTestUtils.getNextTestIndexName());
				sail.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
				sail.setParameter(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "green");
				sail.setParameter(ElasticsearchIndex.WAIT_FOR_NODES_KEY, ">=1");
			}
		};
		delegateTest.setUp();
	}

	@Override
	protected Collection<Class<? extends Plugin>> transportClientPlugins() {
		return Arrays.asList(ReindexPlugin.class);
	}

	@Override
	protected Collection<Class<? extends Plugin>> nodePlugins() {
		return Arrays.asList(ReindexPlugin.class);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			delegateTest.tearDown();
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void testTriplesStored() throws Exception {
		delegateTest.testTriplesStored();
	}

	@Test
	public void testRegularQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testRegularQuery();
	}

	@Test
	public void testComplexQueryOne() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testComplexQueryOne();
	}

	@Test
	public void testComplexQueryTwo() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testComplexQueryTwo();
	}

	@Test
	public void testMultipleLuceneQueries()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testMultipleLuceneQueries();
	}

	@Test
	public void testPredicateLuceneQueries()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testPredicateLuceneQueries();
	}

	@Test
	public void testSnippetQueries() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testSnippetQueries();
	}

	@Test
	public void testSnippetLimitedToPredicate()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testSnippetLimitedToPredicate();
	}

	@Test
	public void testGraphQuery() throws QueryEvaluationException, MalformedQueryException, RepositoryException {
		delegateTest.testGraphQuery();
	}

	@Test
	public void testQueryWithSpecifiedSubject()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testQueryWithSpecifiedSubject();
	}

	@Test
	public void testUnionQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testUnionQuery();
	}

	@Test
	public void testContextHandling() throws Exception {
		delegateTest.testContextHandling();
	}

	@Test
	public void testConcurrentReadingAndWriting() throws Exception {
		delegateTest.testConcurrentReadingAndWriting();
	}

	@Test
	public void testNullContextHandling() throws Exception {
		delegateTest.testNullContextHandling();
	}

	@Test
	public void testFuzzyQuery() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testFuzzyQuery();
	}

	@Test
	public void testReindexing() throws Exception {
		delegateTest.testReindexing();
	}

	@Test
	public void testPropertyVar() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testPropertyVar();
	}

	@Test
	public void testMultithreadedAdd() throws InterruptedException {
		delegateTest.testMultithreadedAdd();
	}

}
