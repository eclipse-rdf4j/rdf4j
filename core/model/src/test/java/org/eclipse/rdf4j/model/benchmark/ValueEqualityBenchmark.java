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

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
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

@SuppressWarnings("UseOfObsoleteDateTimeApi")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgs = { "-Xms4G", "-Xmx4G" })
public class ValueEqualityBenchmark {

	private static final long samples = 1_000_000L;

	public static void main(String[] args) throws RunnerException {
		new Runner(new OptionsBuilder()
				.include(ValueEqualityBenchmark.class.getSimpleName())
				.build()
		).run();
	}

	// private ValueFactory factory=SimpleValueFactory.getInstance();
	private final ValueFactory factory = new BenchmarkValueFactory();

	private final BNode bnodeX = factory.createBNode();
	private final BNode bnodeY = factory.createBNode();

	private final BNode bnodeIdX = factory.createBNode(string("id"));
	private final BNode bnodeIdY = factory.createBNode(string("id"));

	private final IRI iriUnaryX = factory.createIRI(string("http://example.com/name"));
	private final IRI iriUnaryY = factory.createIRI(string("http://example.com/name"));

	private final IRI iriBinaryX = factory.createIRI(string("http://example.com/"), string("name"));
	private final IRI iriBinaryY = factory.createIRI(string("http://example.com/"), string("name"));

	private final Literal plainX = factory.createLiteral(string("text"));
	private final Literal plainY = factory.createLiteral(string("text"));

	private final Literal typedX = factory.createLiteral(string("text"), iriUnaryX);
	private final Literal typedY = factory.createLiteral(string("text"), iriUnaryY);

	private final Literal taggedX = factory.createLiteral(string("text"), string("en"));
	private final Literal taggedY = factory.createLiteral(string("text"), string("en"));

	private final Literal booleanX = factory.createLiteral(true);
	private final Literal booleanY = factory.createLiteral(false);

	private final Literal byteX = factory.createLiteral((byte) 100);
	private final Literal byteY = factory.createLiteral((byte) 123);

	private final Literal shortX = factory.createLiteral((short) 100);
	private final Literal shortY = factory.createLiteral((short) 123);

	private final Literal intX = factory.createLiteral(100);
	private final Literal intY = factory.createLiteral(123);

	private final Literal longX = factory.createLiteral(100L);
	private final Literal longY = factory.createLiteral(123L);

	private final Literal floatX = factory.createLiteral(100.0F);
	private final Literal floatY = factory.createLiteral(123.0F);

	private final Literal doubleX = factory.createLiteral(100.0D);
	private final Literal doubleY = factory.createLiteral(123.0D);

	private final Literal integerX = factory.createLiteral(new BigInteger("100"));
	private final Literal integerY = factory.createLiteral(new BigInteger("100"));

	private final Literal decimalX = factory.createLiteral(new BigDecimal("100"));
	private final Literal decimalY = factory.createLiteral(new BigDecimal("100"));

	private final Literal calendarX = factory.createLiteral(calendar("2020-10-22T15:53:12.345Z"));
	private final Literal calendarY = factory.createLiteral(calendar("2020-10-22T15:53:12.345Z"));

	private final Literal dateX = factory.createLiteral(new Date(1_000_000L));
	private final Literal dateY = factory.createLiteral(new Date(1_000_000L));

	private final Triple tripleX = factory.createTriple(iriUnaryX, iriUnaryX, iriUnaryX);
	private final Triple tripleY = factory.createTriple(iriUnaryY, iriUnaryY, iriUnaryY);

	private final Statement statementX = factory.createStatement(iriUnaryX, iriUnaryX, iriUnaryX);
	private final Statement statementY = factory.createStatement(iriUnaryY, iriUnaryY, iriUnaryY);

	private final Statement statementContextX = factory.createStatement(iriUnaryX, iriUnaryX, iriUnaryX, iriUnaryX);
	private final Statement statementContextY = factory.createStatement(iriUnaryY, iriUnaryY, iriUnaryY, iriUnaryY);

	private final Object[] values = {

			bnodeX, bnodeY,
			bnodeIdX, bnodeIdY,

			iriUnaryX, iriUnaryY,
			iriBinaryX, iriBinaryY,

			plainX, plainY,
			typedX, typedY,
			taggedX, taggedY,

			booleanX, booleanY,

			byteX, byteY,
			shortX, shortY,
			intX, intY,
			longX, longY,
			floatX, floatY,
			doubleX, doubleY,
			integerX, integerY,
			decimalX, decimalY,

			calendarX, calendarY,
			dateX, dateY,

			tripleX, tripleY,

			statementX, statementY,
			statementContextX, statementContextY,

	};

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Benchmark
	public void compareBNode() {
		for (long c = samples; c > 0; --c) {
			bnodeX.equals(bnodeY);
		}
	}

	@Benchmark
	public void compareBNodeId() {
		for (long c = samples; c > 0; --c) {
			bnodeIdX.equals(bnodeIdY);
		}
	}

	@Benchmark
	public void compareIRIUnary() {
		for (long c = samples; c > 0; --c) {
			iriUnaryX.equals(iriUnaryY);
		}
	}

	@Benchmark
	public void compareIRIBinary() {
		for (long c = samples; c > 0; --c) {
			iriBinaryX.equals(iriBinaryY);
		}
	}

	@Benchmark
	public void comparePlain() {
		for (long c = samples; c > 0; --c) {
			plainX.equals(plainY);
		}
	}

	@Benchmark
	public void compareTyped() {
		for (long c = samples; c > 0; --c) {
			typedX.equals(typedY);
		}
	}

	@Benchmark
	public void compareTagged() {
		for (long c = samples; c > 0; --c) {
			taggedX.equals(taggedY);
		}
	}

	@Benchmark
	public void compareBoolean() {
		for (long c = samples; c > 0; --c) {
			booleanX.equals(booleanY);
		}
	}

	@Benchmark
	public void compareByte() {
		for (long c = samples; c > 0; --c) {
			byteX.equals(byteY);
		}
	}

	@Benchmark
	public void compareShort() {
		for (long c = samples; c > 0; --c) {
			shortX.equals(shortY);
		}
	}

	@Benchmark
	public void compareInt() {
		for (long c = samples; c > 0; --c) {
			intX.equals(intY);
		}
	}

	@Benchmark
	public void compareLong() {
		for (long c = samples; c > 0; --c) {
			longX.equals(longY);
		}
	}

	@Benchmark
	public void compareFloat() {
		for (long c = samples; c > 0; --c) {
			floatX.equals(floatY);
		}
	}

	@Benchmark
	public void compareDouble() {
		for (long c = samples; c > 0; --c) {
			doubleX.equals(doubleY);
		}
	}

	@Benchmark
	public void compareInteger() {
		for (long c = samples; c > 0; --c) {
			integerX.equals(integerY);
		}
	}

	@Benchmark
	public void compareDecimal() {
		for (long c = samples; c > 0; --c) {
			decimalX.equals(decimalY);
		}
	}

	@Benchmark
	public void compareCalendar() {
		for (long c = samples; c > 0; --c) {
			calendarX.equals(calendarY);
		}
	}

	@Benchmark
	public void compareDate() {
		for (long c = samples; c > 0; --c) {
			dateX.equals(dateY);
		}
	}

	@Benchmark
	public void compareTriple() {
		for (long c = samples; c > 0; --c) {
			tripleX.equals(tripleY);
		}
	}

	@Benchmark
	public void compareStatement() {
		for (long c = samples; c > 0; --c) {
			statementX.equals(statementY);
		}
	}

	@Benchmark
	public void compareStatementContext() {
		for (long c = samples; c > 0; --c) {
			statementContextX.equals(statementContextY);
		}
	}

	@Benchmark
	public void compareValues() {
		for (long c = samples / (values.length * values.length); c > 0; --c) {
			for (Object x : values) {
				for (Object y : values) {
					x.equals(y);
				}
			}
		}
	}

	private static String string(String string) {
		return new String(string); // force unique object creation
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
