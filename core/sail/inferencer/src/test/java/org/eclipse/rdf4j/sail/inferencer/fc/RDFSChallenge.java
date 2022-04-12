/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RDFSChallenge {
	public static void main(String[] args) throws IOException {

		StopWatch stopWatch = StopWatch.createStarted();
		((Logger) LoggerFactory.getLogger(SchemaCachingRDFSInferencer.class.getName())).setLevel(Level.DEBUG);
		((Logger) LoggerFactory.getLogger(SchemaCachingRDFSInferencerConnection.class.getName())).setLevel(Level.DEBUG);
		File tempDir = Files.createTempDir();
		System.out.println(tempDir.getAbsolutePath());

		SailRepository repository = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			System.out.println("Add abox");
			connection.add(RDFSChallenge.class.getClassLoader().getResource("challenge/abox.ttl"));
			System.out.println("Add tbox");
			connection.add(RDFSChallenge.class.getClassLoader().getResource("challenge/tbox.ttl"));
			System.out.println("Commit");
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			System.out.println("Query");
			TupleQuery tupleQuery = connection.prepareTupleQuery(
					"PREFIX  wd:   <http://www.wikidata.org/entity/>\n" + "PREFIX  ex:   <http://example.com/>\n"
							+ "SELECT  *\n" + "WHERE\n" + "  { ex:condition0 a ?type\n" + "  }");
			try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
				List<BindingSet> collect = evaluate.stream().collect(Collectors.toList());
				collect.forEach(System.out::println);
				System.out.println(collect.size());
			}
		}

		repository.shutDown();
		stopWatch.stop();
		System.out.println("\nTook: " + stopWatch);
	}

}
