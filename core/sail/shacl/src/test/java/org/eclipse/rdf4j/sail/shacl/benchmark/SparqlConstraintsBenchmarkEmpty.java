/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xmx64M" })
//@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SparqlConstraintsBenchmarkEmpty {

	private List<List<Statement>> allStatements;

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		System.gc();
		Thread.sleep(100);
	}

	@Benchmark
	public void shacl() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("benchmark/sparql/shacl.trig"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (int i = 0; i < 10; i++) {
				connection.begin();
				URL data = SparqlConstraintsBenchmarkEmpty.class.getClassLoader()
						.getResource("benchmark/sparql/data.ttl");
				connection.add(data, RDFFormat.TURTLE);
				try {
					connection.commit();
				} catch (Exception e) {

					Model statements = ((ShaclSailValidationException) e.getCause()).validationReportAsModel();
					for (Statement statement : statements) {
						System.out.println(statement);
					}
					System.exit(0);
					connection.rollback();
				}
			}
		}
		repository.shutDown();

	}

	@Benchmark
	public void shaclBulk() throws Exception {

		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("benchmark/sparql/shacl.trig"));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			for (int i = 0; i < 10; i++) {
				URL data = SparqlConstraintsBenchmarkEmpty.class.getClassLoader()
						.getResource("benchmark/sparql/data.ttl");
				connection.add(data, RDFFormat.TURTLE);
			}
			connection.commit();
		}
		repository.shutDown();

	}

}
