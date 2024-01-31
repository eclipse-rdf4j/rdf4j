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
package org.eclipse.rdf4j.sail.elasticsearch;

import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.testsuite.rdf4j.sail.lucene.AbstractLuceneSailTest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ClusterScope(numDataNodes = 1)
public class ElasticsearchSailTest extends ESIntegTestCase {

	AbstractLuceneSailTest delegateTest;

	@BeforeEach
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
		return List.of(ReindexPlugin.class);
	}

	@Override
	protected Collection<Class<? extends Plugin>> nodePlugins() {
		return List.of(ReindexPlugin.class);
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		try {
			delegateTest.tearDown();
		} finally {
			super.tearDown();
		}
	}

	@Test
	void testTriplesStored() {
		delegateTest.testTriplesStored();
	}

	@Test
	void testRegularQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testRegularQuery();
	}

	@Test
	void testComplexQueryOne() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testComplexQueryOne();
	}

	@Test
	void testComplexQueryTwo() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testComplexQueryTwo();
	}

	@Test
	void testMultipleLuceneQueries()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testMultipleLuceneQueries();
	}

	@Test
	void testPredicateLuceneQueries()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testPredicateLuceneQueries();
	}

	@Test
	void testSnippetQueries() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testSnippetQueries();
	}

	@Test
	void testSnippetLimitedToPredicate()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testSnippetLimitedToPredicate();
	}

	@Test
	void testGraphQuery() throws QueryEvaluationException, MalformedQueryException, RepositoryException {
		delegateTest.testGraphQuery();
	}

	@Test
	void testQueryWithSpecifiedSubject()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testQueryWithSpecifiedSubject();
	}

	@Test
	void testUnionQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testUnionQuery();
	}

	@Test
	void testContextHandling() {
		delegateTest.testContextHandling();
	}

	@Test
	void testConcurrentReadingAndWriting() {
		delegateTest.testConcurrentReadingAndWriting();
	}

	@Test
	void testNullContextHandling() {
		delegateTest.testNullContextHandling();
	}

	@Test
	void testFuzzyQuery() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testFuzzyQuery();
	}

	@Test
	void testReindexing() {
		delegateTest.testReindexing();
	}

	@Test
	void testPropertyVar() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		delegateTest.testPropertyVar();
	}

	@Test
	void testMultithreadedAdd() throws InterruptedException {
		delegateTest.testMultithreadedAdd();
	}

}
