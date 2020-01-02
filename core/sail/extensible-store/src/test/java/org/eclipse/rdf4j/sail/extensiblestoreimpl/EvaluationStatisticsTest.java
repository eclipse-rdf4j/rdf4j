/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.commons.io.IOUtils;
import org.apache.druid.hll.HyperLogLogCollector;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.ExtensibleDynamicEvaluationStatistics;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertEquals;

public class EvaluationStatisticsTest {

	private static final Logger logger = LoggerFactory.getLogger(EvaluationStatisticsTest.class);

	@Test
	public void temp() {

		ExtensibleStoreImplForTests extensibleStoreImplForTests = new ExtensibleStoreImplForTests();

		SailRepository sailRepository = new SailRepository(extensibleStoreImplForTests);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.commit();
		}

		sailRepository.shutDown();
	}

	@Test
	public void hllTest() {

		Statement statement = SimpleValueFactory.getInstance().createStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		HyperLogLogCollector collector = HyperLogLogCollector.makeLatestCollector();

		HashFunction hashFunction = Hashing.murmur3_128();
		collector.add(hashFunction.hashString(statement.toString(), StandardCharsets.UTF_8).asBytes());

		double cardinality = collector.estimateCardinality();

		collector.add(hashFunction.hashString(statement.toString(), StandardCharsets.UTF_8).asBytes());

		assertEquals(cardinality, collector.estimateCardinality());

	}

	@Test
	public void queryPlanTest() throws IOException {

		ExtensibleStoreImplForTests extensibleStoreImplForTests = new ExtensibleStoreImplForTests();

		SailRepository sailRepository = new SailRepository(extensibleStoreImplForTests);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(EvaluationStatisticsTest.class.getClassLoader().getResourceAsStream("bsbm-100.ttl"), "",
					RDFFormat.TURTLE);
			connection.commit();

			TupleQuery tupleQuery = connection.prepareTupleQuery(getQuery("evaluation-statistics/query1.rq"));
			System.out.println(tupleQuery.toString());
			try (IteratingTupleQueryResult evaluate = (IteratingTupleQueryResult) tupleQuery.evaluate()) {
				System.out.println(evaluate.toString());
				long count = Iterations.stream(evaluate).count();
				System.out.println(count);
			}

		}

		String distribution = ((ExtensibleDynamicEvaluationStatistics) extensibleStoreImplForTests.getEvalStats())
				.getDistribution();

		System.out.println(distribution);

		sailRepository.shutDown();
	}

	private String getQuery(String name) throws IOException {
		return IOUtils.toString(EvaluationStatisticsTest.class.getClassLoader().getResourceAsStream(name),
				StandardCharsets.UTF_8);
	}

}
