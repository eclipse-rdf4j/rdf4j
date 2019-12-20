/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import ch.qos.logback.classic.Logger;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
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

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=ProfilingAggressive.jfc", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ShaclLoadingBenchmark {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Setup(Level.Iteration)
	public void setUp() {
		System.gc();
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);
	}

	@Benchmark
	public void testAddingDataAndAddingShapes() throws Exception {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));
		repository.init();
		((ShaclSail) repository.getSail()).setParallelValidation(true);
		((ShaclSail) repository.getSail()).setCacheSelectNodes(true);

		try (SailRepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.SNAPSHOT);

			for (int i = 0; i < BenchmarkConfigs.NUMBER_OF_TRANSACTIONS; i++) {
				StringReader data = new StringReader(String.join("\n", "",
						"@prefix ex: <http://example.com/ns#> .",
						"@prefix sh: <http://www.w3.org/ns/shacl#> .",
						"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
						"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " .",
						"[] a ex:Person; ",
						"	ex:age" + i + " " + i + " ."

				));

				connection.add(data, "", RDFFormat.TURTLE);

			}
			connection.commit();

			for (int i = 0; i < BenchmarkConfigs.NUMBER_OF_TRANSACTIONS; i++) {
				connection.begin(IsolationLevels.SNAPSHOT);
				StringReader shaclRules = new StringReader(String.join("\n", "",
						"@prefix ex: <http://example.com/ns#> .",
						"@prefix sh: <http://www.w3.org/ns/shacl#> .",
						"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
						"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

						"[]",
						"        a sh:NodeShape  ;",
						"        sh:targetClass ex:Person ;",
						"        sh:property [",
						"                sh:path ex:age" + i + " ;",
						"                sh:datatype xsd:integer ;",
						"        ] ."));

				connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
				connection.commit();

			}
		}

		repository.shutDown();

	}

}
