// File: src/jmh/java/org/eclipse/rdf4j/query/algebra/evaluation/benchmark/GeneralCompareBench.java
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
package org.eclipse.rdf4j.query.algebra.evaluation.benchmark;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 6)
@Measurement(iterations = 10)
@Fork(2)
public class GeneralCompareBench {

	@State(Scope.Benchmark)
	public static class DataSet {
		@Param({ "65536" }) // large enough to avoid cache re-use patterns
		public int size;

		@Param({ "42" })
		public long seed;

		/**
		 * Percentage (0..100) of items that are intentionally error cases (e.g., incompatible supported types in strict
		 * mode, unsupported datatypes, indeterminate dateTime).
		 */
		@Param({ "3" })
		public int errorRatePercent;

		/**
		 * Distribution profile: - "balanced": a bit of everything - "numericHeavy": more numbers - "stringHeavy": more
		 * strings
		 */
		@Param({ "balanced" })
		public String mix;

		Value[] a;
		Value[] b;
		CompareOp[] op;
		boolean[] strict;

		final SimpleValueFactory vf = SimpleValueFactory.getInstance();
		DatatypeFactory df;
		IRI unknownDT;

		@Setup
		public void setup() {
			try {
				df = DatatypeFactory.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			unknownDT = vf.createIRI("http://example.com/dt#unknown");

			a = new Value[size];
			b = new Value[size];
			op = new CompareOp[size];
			strict = new boolean[size];

			Random rnd = new Random(seed);

			int wNum, wStr, wBool, wDate, wDur, wUnsup, wIncomp;
			switch (mix) {
			case "numericHeavy": {
				wNum = 55;
				wStr = 10;
				wBool = 5;
				wDate = 15;
				wDur = 5;
				wUnsup = 5;
				wIncomp = 5;
			}
				break;
			case "stringHeavy": {
				wNum = 15;
				wStr = 55;
				wBool = 5;
				wDate = 10;
				wDur = 5;
				wUnsup = 5;
				wIncomp = 5;
			}
				break;
			default: {
				wNum = 35;
				wStr = 25;
				wBool = 10;
				wDate = 15;
				wDur = 5;
				wUnsup = 5;
				wIncomp = 5;
			}
				break;
			}
			final int total = wNum + wStr + wBool + wDate + wDur + wUnsup + wIncomp;

			for (int i = 0; i < size; i++) {
				// Generate a pair (a[i], b[i]) of some type
				int pick = rnd.nextInt(total);
				boolean isDuration = false;
				if ((pick -= wNum) < 0) {
					genNumeric(i, rnd);
				} else if ((pick -= wStr) < 0) {
					genString(i, rnd);
				} else if ((pick -= wBool) < 0) {
					genBoolean(i, rnd);
				} else if ((pick -= wDate) < 0) {
					genDateTime(i, rnd);
				} else if ((pick -= wDur) < 0) {
					genDuration(i, rnd);
					isDuration = true; // this type requires non-strict to hit the duration path
				} else if ((pick -= wUnsup) < 0) {
					genUnsupported(i, rnd);
				} else {
					genIncompatibleSupported(i, rnd);
				}

				// Choose operator
				op[i] = CompareOp.values()[rnd.nextInt(CompareOp.values().length)];

				// Choose strictness (duration items force non-strict so the duration code path is actually exercised)
				strict[i] = isDuration ? false : rnd.nextInt(100) >= 15;

				// Inject a small fraction of explicit error cases (overrides everything above)
				if (rnd.nextInt(100) < errorRatePercent) {
					int mode = rnd.nextInt(3);
					switch (mode) {
					case 0: { // string vs boolean under strict EQ/NE -> strict type error
						a[i] = vf.createLiteral("foo");
						b[i] = vf.createLiteral(rnd.nextBoolean());
						op[i] = rnd.nextBoolean() ? CompareOp.EQ : CompareOp.NE;
						strict[i] = true;
					}
						break;
					case 1: { // dateTime indeterminate: no-tz vs Z under strict -> INDETERMINATE thrown
						a[i] = vf.createLiteral(df.newXMLGregorianCalendar("2020-01-01T00:00:00"));
						b[i] = vf.createLiteral(df.newXMLGregorianCalendar("2020-01-01T00:00:00Z"));
						op[i] = CompareOp.EQ;
						strict[i] = true;
					}
						break;
					default: { // unsupported datatypes
						a[i] = vf.createLiteral("x", unknownDT);
						b[i] = vf.createLiteral("y", unknownDT);
						op[i] = CompareOp.EQ;
						strict[i] = true;
					}
					}
				}
			}
		}

		private void genNumeric(int i, Random rnd) {
			int subtype = rnd.nextInt(4); // 0:double, 1:float, 2:integer, 3:decimal
			switch (subtype) {
			case 0: {
				double x = rnd.nextDouble() * 1e6 - 5e5;
				double y = rnd.nextInt(10) == 0 ? x : x + (rnd.nextBoolean() ? 1 : -1) * rnd.nextDouble();
				a[i] = vf.createLiteral(x);
				b[i] = vf.createLiteral(y);
			}
				break;
			case 1: {
				float x = (float) (rnd.nextGaussian() * 100.0);
				float y = rnd.nextInt(10) == 0 ? x : x + (rnd.nextBoolean() ? 1 : -1) * (float) rnd.nextGaussian();
				a[i] = vf.createLiteral(x);
				b[i] = vf.createLiteral(y);
			}
				break;
			case 2: {
				BigInteger x = new BigInteger(64, rnd);
				BigInteger y = rnd.nextInt(10) == 0 ? x : x.add(BigInteger.valueOf(rnd.nextInt(3) - 1));
				a[i] = vf.createLiteral(x);
				b[i] = vf.createLiteral(y);
			}
				break;
			default: {
				// decimals with varying scale
				BigDecimal x = new BigDecimal(String.format("%d.%02d", rnd.nextInt(1000), rnd.nextInt(100)));
				BigDecimal y = rnd.nextInt(10) == 0 ? x : x.add(new BigDecimal("0.01"));
				a[i] = vf.createLiteral(x);
				b[i] = vf.createLiteral(y);
			}
			}
		}

		private void genString(int i, Random rnd) {
			String[] pool = { "a", "b", "foo", "bar", "lorem", "ipsum", "" };
			String x = pool[rnd.nextInt(pool.length)];
			String y = rnd.nextInt(10) == 0 ? x : pool[rnd.nextInt(pool.length)];
			a[i] = vf.createLiteral(x); // xsd:string (simple)
			b[i] = vf.createLiteral(y);
		}

		private void genBoolean(int i, Random rnd) {
			boolean x = rnd.nextBoolean();
			boolean y = rnd.nextInt(10) == 0 ? x : !x;
			a[i] = vf.createLiteral(x);
			b[i] = vf.createLiteral(y);
		}

		private void genDateTime(int i, Random rnd) {
			// Three variants:
			// 0) Z vs Z (equal)
			// 1) +01:00 vs Z but same instant (12:..+01:00 equals 11:..Z) <-- fixed: adjust hour, not minutes
			// 2) no tz vs Z (often INDETERMINATE under strict)
			int m = rnd.nextInt(60), s = rnd.nextInt(60);
			String xLex, yLex;
			switch (rnd.nextInt(3)) {
			case 0: {
				xLex = String.format("2020-01-01T12:%02d:%02dZ", m, s);
				yLex = xLex;
			}
				break;
			case 1: {
				xLex = String.format("2020-01-01T12:%02d:%02d+01:00", m, s);
				yLex = String.format("2020-01-01T11:%02d:%02dZ", m, s); // same instant, valid time
			}
				break;
			default: {
				xLex = String.format("2020-01-01T12:%02d:%02d", m, s); // no tz
				yLex = String.format("2020-01-01T12:%02d:%02dZ", m, s); // Z
			}
				break;
			}
			XMLGregorianCalendar x = df.newXMLGregorianCalendar(xLex);
			XMLGregorianCalendar y = df.newXMLGregorianCalendar(yLex);
			a[i] = vf.createLiteral(x);
			b[i] = vf.createLiteral(y);
		}

		private void genDuration(int i, Random rnd) {
			// Common equal-ish durations (P1D vs PT24H) and slight differences
			boolean equal = rnd.nextBoolean();
			String x = "P1D";
			String y = equal ? "PT24H" : "PT24H30M";
			a[i] = vf.createLiteral(x, CoreDatatype.XSD.DURATION.getIri());
			b[i] = vf.createLiteral(y, CoreDatatype.XSD.DURATION.getIri());
			// strictness is handled by caller (forced false for durations)
		}

		private void genUnsupported(int i, Random rnd) {
			a[i] = vf.createLiteral("x", unknownDT);
			b[i] = vf.createLiteral("y", unknownDT);
		}

		private void genIncompatibleSupported(int i, Random rnd) {
			// e.g., xsd:string vs xsd:boolean (supported but incompatible)
			a[i] = vf.createLiteral("foo");
			b[i] = vf.createLiteral(rnd.nextBoolean());
		}
	}

	@State(Scope.Thread)
	public static class Cursor {
		int idx = 0;
		boolean pow2;
		int mask;

		@Setup(Level.Iteration)
		public void setup(DataSet ds) {
			idx = 0;
			pow2 = (ds.size & (ds.size - 1)) == 0;
			mask = ds.size - 1;
		}

		int next(int n) {
			int i = idx++;
			if (pow2) {
				idx &= mask;
				return i & mask;
			} else {
				// Avoid expensive % in hot loop: manual wrap
				if (idx >= n)
					idx -= n;
				return (i >= n) ? (i - n) : i;
			}
		}
	}

	@Benchmark
	public void general_dispatch_compare(DataSet ds, Cursor cur, Blackhole bh) {
		final int i = cur.next(ds.size);
		boolean r = false;
		try {
			r = QueryEvaluationUtil.compare(ds.a[i], ds.b[i], ds.op[i], ds.strict[i]);
		} catch (ValueExprEvaluationException ex) {
			bh.consume(ex.getClass());
		}
		bh.consume(r);
	}

	@Benchmark
	public void general_literal_EQ_fastpath(DataSet ds, Cursor cur, Blackhole bh) {
		final int i = cur.next(ds.size);
		boolean r = false;
		try {
			r = QueryEvaluationUtil.compareLiteralsEQ((Literal) ds.a[i], (Literal) ds.b[i], ds.strict[i]);
		} catch (Throwable t) {
			bh.consume(t.getClass());
		}
		bh.consume(r);
	}
}
