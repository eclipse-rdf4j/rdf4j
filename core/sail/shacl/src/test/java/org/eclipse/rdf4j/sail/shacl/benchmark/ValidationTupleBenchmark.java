/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
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
 * Test how many validation tuples we can keep in memory.
 *
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms512M", "-Xmx512M" })
//@Fork(value = 1, jvmArgs = {"-Xms512M", "-Xmx512M",  "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ValidationTupleBenchmark {

	private static final String NS1 = "http://example.com/fkewjfowejiofiew/fjewifoweifjwe/jfiewjifjewofiwe/";
	private static final String NS2 = "http://example.com/jiu98u89/fjewifoweifjwe/jfiewjifjewofiwe/";
	private static final String NS3 = "http://example.com/fkewjfowejiofiew/556r6fuig7t87/jfiewjifjewofiwe/";
	public static final Resource[] CONTEXTS = { null };

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		Thread.sleep(100);
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName())).setLevel(ch.qos.logback.classic.Level.ERROR);

	}

	@Benchmark
	public int randomData() {

		int size = 670_000;

		ValueFactory vf = SimpleValueFactory.getInstance();

		Random r = new Random(5637248);

		ArrayList<Object> objects = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			List<Value> values = Arrays.asList(
					vf.createIRI(NS1 + r.nextInt(size * 1000)),
					vf.createIRI(NS2 + r.nextInt(size * 1000)),
					vf.createIRI(NS3 + r.nextInt(size * 1000)),
					vf.createLiteral(NS3 + r.nextInt(size * 1000)),
					vf.createLiteral(r.nextInt(size * 1000))
			);

			ValidationTuple validationTuple = new ValidationTuple(values, ConstraintComponent.Scope.propertyShape,
					true, CONTEXTS);
			objects.add(validationTuple);

		}

		return objects.size();

	}

}
