/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util.benchmark;

import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.XMLDatatypeMathUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-XX:+UseSerialGC" })
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MathUtilBenchmark {
	private static SimpleValueFactory svf = SimpleValueFactory.getInstance();
	@Param({ "100", "200", "300", "500", "100000" })
	public int iterations;

	@Benchmark
	public int intSum() {
		Literal t = svf.createLiteral(0);
		for (int i = 0; i < iterations; i++) {
			Literal l = svf.createLiteral(i);
			t = MathUtil.compute(l, t, MathOp.PLUS, svf);
		}
		return t.intValue();
	}

	@Benchmark
	public int intSumXMLDatatype() {
		Literal t = svf.createLiteral(0);
		for (int i = 0; i < iterations; i++) {
			Literal l = svf.createLiteral(i);
			t = XMLDatatypeMathUtil.compute(l, t, MathOp.PLUS, svf);
		}
		return t.intValue();
	}
}
