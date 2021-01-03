/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IsomorphicBenchmark {

	private Model empty = getModel("empty.ttl");
	private Model blankNodes = getModel("blankNodes.ttl");
	private Model shacl = getModel("shacl.ttl");
	private Model shaclValidationReport = getModel("shaclValidationReport.ttl");
	private Model longChain = getModel("longChain.ttl");
	private Model sparqlTestCase = getModel("sparqlTestCase.ttl");
	private Model spinFullForwardchained = getModel("spin-full-forwardchained.ttl");
	private Model bsbm = getModel("bsbm-100.ttl");
	private Model bsbmChanged = getModel("bsbm-100-changed.ttl");
	private List<Statement> bsbm_arraylist = new ArrayList<>(bsbm);
	private Model bsbmTree = new TreeModel(bsbm);
	private Model list = getModel("list.ttl");
	private Model internallyIsomorphic = getModel("internallyIsomorphic.ttl");
	private Model manyProperties = getModel("manyProperties.ttl");
	private Model manyProperties2 = getModel("manyProperties2.ttl");
	private Model uuid = getModel("uuid.ttl");

	private Model empty_2 = getModel("empty.ttl");
	private Model blankNodes_2 = getModel("blankNodes.ttl");
	private Model shacl_2 = getModel("shacl.ttl");
	private Model shaclValidationReport_2 = getModel("shaclValidationReport.ttl");
	private Model longChain_2 = getModel("longChain.ttl");
	private Model sparqlTestCase_2 = getModel("sparqlTestCase.ttl");
	private Model spinFullForwardchained_2 = getModel("spin-full-forwardchained.ttl");
	private Model bsbm_2 = getModel("bsbm-100.ttl");
	private List<Statement> bsbm_arraylist_2 = new ArrayList<>(bsbm);
	private Model bsbmTree_2 = new TreeModel(bsbm);
	private Model list_2 = getModel("list.ttl");
	private Model internallyIsomorphic_2 = getModel("internallyIsomorphic.ttl");
	private Model manyProperties_2 = getModel("manyProperties.ttl");
	private Model manyProperties2_2 = getModel("manyProperties2.ttl");
	private Model uuid_2 = getModel("uuid.ttl");

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include("IsomorphicBenchmark.*")
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Iteration)
	public void after() {
		System.gc();
	}

	// checks for optimisation when comparing the same objects
	@Benchmark
	public boolean sameModel() {

		return isomorphic(bsbm, bsbm);

	}

	// check performance of an empty model
	@Benchmark
	public boolean empty() {

		return isomorphic(empty, empty_2);

	}

	// check performance when using UUIDs
	@Benchmark
	public boolean uuid() {

		return isomorphic(uuid, uuid_2);

	}

	// checks performance for a model with many blank nodes
	@Benchmark
	public boolean blankNodes() {

		return isomorphic(blankNodes, blankNodes_2);

	}

	// checks performance for a typical SHACL file (with nested blank nodes)
	@Benchmark
	public boolean shacl() {

		return isomorphic(shacl, shacl_2);

	}

	// checks performance for a typical SHACL validation report
	@Benchmark
	public boolean shaclValidationREport() {
		return isomorphic(shaclValidationReport, shaclValidationReport_2);
	}

	// checks performance for a long chaing of rdfs:subClassOf statements
	@Benchmark
	public boolean longChain() {
		return isomorphic(longChain, longChain_2);
	}

	// checks performance for a file used in the SPARQL compliance tests
	@Benchmark
	public boolean sparqlTestCase() {

		return isomorphic(sparqlTestCase, sparqlTestCase_2);

	}

	// checks performance for a rather large and varied file
	@Benchmark
	public boolean bsbm() {

		return isomorphic(bsbm, bsbm_2);

	}

	// checks performance of a the same file as above, but this time using a TreeModel instead of the default
	// LinkedHashModel
	@Benchmark
	public boolean bsbmTree() {

		return isomorphic(bsbmTree, bsbmTree_2);

	}

	// checks performance when isomorphic is called with two array lists instead of models
	@Benchmark
	public boolean bsbmArrayList() {

		boolean isomorphic = Models.isomorphic(bsbm_arraylist, bsbm_arraylist_2);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

		return isomorphic;

	}

	// checks performance for the fully forward chained version of the base SPIN file
	@Benchmark
	public boolean spinFullForwardchained() {

		return isomorphic(spinFullForwardchained, spinFullForwardchained_2);

	}

	// checks performance for varied use of RDF lists
	@Benchmark
	public boolean list() {

		return isomorphic(list, list_2);

	}

	// checks performance of SimpleBNode.equals(...)
	@Benchmark
	public int listEquals() {

		int i = 0;
		for (Statement statement : list) {
			for (Statement statement1 : list_2) {
				if (statement.getSubject().equals(statement1.getSubject())) {
					i++;
				}
			}
		}

		return i;

	}

	// checks performance for a file that has multiple internal isomorphisms
	@Benchmark
	public boolean internallyIsomorphic() {

		return isomorphic(internallyIsomorphic, internallyIsomorphic_2);

	}

	// checks performance on a file with many unique properties (predicates) and also with blank nodes, IRIs and
	// literals
	@Benchmark
	public boolean manyProperties() {

		return isomorphic(manyProperties, manyProperties_2);

	}

	// checks performance on a file with many unique properties (predicates) and also with blank nodes
	@Benchmark
	public boolean manyProperties2() {

		return isomorphic(manyProperties2, manyProperties2_2);

	}

	// checks perfomance of comparing an empty model to a large model
	@Benchmark
	public boolean emptyNotIsomorphic() {

		return notIsomorphic(empty, bsbm);

	}

	// checks performance of comparing two models that are equals except for one statement (same sizes)
	@Benchmark
	public boolean bsbmNotIsomorphic() {

		return notIsomorphic(bsbm, bsbmChanged);

	}

	private Model getModel(String name) {
		try {
			try (InputStream resourceAsStream = IsomorphicBenchmark.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/" + name)) {
				return Rio.parse(resourceAsStream, "http://example.com/", RDFFormat.TURTLE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isomorphic(Model m1, Model m2) {

		boolean isomorphic = Models.isomorphic(m1, m2);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

		return isomorphic;
	}

	private boolean notIsomorphic(Model m1, Model m2) {

		boolean isomorphic = Models.isomorphic(m1, m2);
		if (isomorphic) {
			throw new IllegalStateException("Should not be isomorphic");
		}

		return isomorphic;
	}

}
