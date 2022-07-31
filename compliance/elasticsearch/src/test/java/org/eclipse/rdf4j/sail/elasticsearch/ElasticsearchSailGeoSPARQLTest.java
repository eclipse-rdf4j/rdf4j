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
import org.eclipse.testsuite.rdf4j.sail.lucene.AbstractLuceneSailGeoSPARQLTest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@ClusterScope(numDataNodes = 1)
public class ElasticsearchSailGeoSPARQLTest extends ESIntegTestCase {

	AbstractLuceneSailGeoSPARQLTest delegateTest;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		TransportClient client = (TransportClient) internalCluster().transportClient();
		delegateTest = new AbstractLuceneSailGeoSPARQLTest() {

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
	public void testDistanceQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testDistanceQuery();
	}

	@Test
	public void testComplexDistanceQuery()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testComplexDistanceQuery();
	}

	@Test
	@Ignore // JTS is required
	public void testIntersectionQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testIntersectionQuery();
	}

	@Test
	@Ignore // JTS is required
	public void testComplexIntersectionQuery()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		delegateTest.testComplexIntersectionQuery();
	}
}
