/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.benchmark;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgs = { "-Xms4G", "-Xmx4G" })
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ValueCreationBenchmark {

	private static final long samples = 1_000_000L;

	public static void main(String[] args) throws RunnerException {
		new Runner(new OptionsBuilder()
				.include(ValueCreationBenchmark.class.getSimpleName())
				.build()
		).run();
	}

	// private ValueFactory factory=SimpleValueFactory.getInstance();
	private final ValueFactory factory = new BenchmarkValueFactory();

	private final IRI iri = factory.createIRI("http://example.com/name");

	private final XMLGregorianCalendar calendar = calendar("2020-10-22T15:53:12.345Z");
	private final Date date = new Date();

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Benchmark
	public void createBNode() {
		for (long c = samples; c > 0; --c) {
			factory.createBNode();
		}
	}

	@Benchmark
	public void createBNodeId() {
		for (long c = samples; c > 0; --c) {
			factory.createBNode("123");
		}
	}

	@Benchmark
	public void createIRIUnary() {
		for (long c = samples; c > 0; --c) {
			factory.createIRI("http://example.com/name");
		}
	}

	@Benchmark
	public void createIRIBinary() {
		for (long c = samples; c > 0; --c) {
			factory.createIRI("http://example.com/", "name");
		}
	}

	@Benchmark
	public void createLiteralPlain() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral("text");
		}
	}

	@Benchmark
	public void createLiteralTagged() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral("text", "en");
		}
	}

	@Benchmark
	public void createLiteralTyped() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral("text", iri);
		}
	}

	@Benchmark
	public void createLiteralBoolean() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(true);
		}
	}

	@Benchmark
	public void createLiteralByte() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral((byte) 100);
		}
	}

	@Benchmark
	public void createLiteralShort() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral((short) 100);
		}
	}

	@Benchmark
	public void createLiteralInt() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(100);
		}
	}

	@Benchmark
	public void createLiteralLong() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(100L);
		}
	}

	@Benchmark
	public void createLiteralFloat() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(100F);
		}
	}

	@Benchmark
	public void createLiteralDouble() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(100D);
		}
	}

	@Benchmark
	public void createLiteralInteger() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(BigInteger.TEN);
		}
	}

	@Benchmark
	public void createLiteralDecimal() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(BigDecimal.TEN);
		}
	}

	@Benchmark
	public void createLiteralCalendar() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(calendar);
		}
	}

	@Benchmark
	public void createLiteralDate() {
		for (long c = samples; c > 0; --c) {
			factory.createLiteral(date);
		}
	}

	@Benchmark
	public void createTriple() {
		for (long c = samples; c > 0; --c) {
			factory.createTriple(iri, iri, iri);
		}
	}

	@Benchmark
	public void createStatement() {
		for (long c = samples; c > 0; --c) {
			factory.createStatement(iri, iri, iri);
		}
	}

	@Benchmark
	public void createStatementContext() {
		for (long c = samples; c > 0; --c) {
			factory.createStatement(iri, iri, iri, iri);
		}
	}

	private XMLGregorianCalendar calendar(String string) {
		try {

			return DatatypeFactory.newInstance().newXMLGregorianCalendar(string);

		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private static class BenchmarkValueFactory extends AbstractValueFactory {
	}

}
