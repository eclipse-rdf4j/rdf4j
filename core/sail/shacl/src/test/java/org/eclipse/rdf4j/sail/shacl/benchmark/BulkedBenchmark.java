/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
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
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BulkedBenchmark {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	private final static int SIZE = 10000;
	private static final String QUERY = "?a <" + RDFS.LABEL + "> ?c";

	private final SailRepository repository = new SailRepository(new MemoryStore());
//	private final List<ValidationTuple> subjects;
//
//	public BulkedBenchmark() {
//
//		repository.init();
//
//		List<Resource> subjects = new ArrayList<>();
//
//		try (SailRepositoryConnection connection = repository.getConnection()) {
//			connection.begin();
//			ValueFactory vf = connection.getValueFactory();
//			for (int i = 0; i < SIZE; i++) {
//				IRI iri = vf.createIRI("http://example.com/" + i);
//				connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
//				connection.add(iri, RDFS.LABEL, vf.createLiteral("label_" + i));
//				subjects.add(iri);
//			}
//
//			connection.commit();
//		}
//
//		ValueComparator valueComparator = new ValueComparator();
//		subjects.sort(valueComparator);
//
//		this.subjects = subjects.stream().map(Tuple::new).collect(Collectors.toList());
//
//	}
//
//	@Setup(Level.Invocation)
//	public void setUp() throws InterruptedException {
//		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
//		root.setLevel(ch.qos.logback.classic.Level.INFO);
//		System.gc();
//		Thread.sleep(100);
//	}
//
//	@Benchmark
//	public int innerJoin() {
//		try (SailConnection connection = repository.getSail().getConnection()) {
//			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(new MockInputPlanNode(subjects), connection,
//					QUERY, false, null, "?a", "?c");
//			return new MockConsumePlanNode(bulkedExternalInnerJoin).asList().size();
//		}
//	}
//
//	@Benchmark
//	public int outerJoin() {
//		try (SailConnection connection = repository.getSail().getConnection()) {
//			PlanNode bulkedExternalInnerJoin = new BulkedExternalLeftOuterJoin(new MockInputPlanNode(subjects),
//					connection, QUERY, false, null, "?a", "?c");
//			return new MockConsumePlanNode(bulkedExternalInnerJoin).asList().size();
//		}
//	}

}
