/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import static junit.framework.TestCase.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluationStatisticsTest {

	private static final Logger logger = LoggerFactory.getLogger(EvaluationStatisticsTest.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
	private static final ExtensibleStatementHelper ex = ExtensibleStatementHelper.getDefaultImpl();

	Model parse;

	{
		try {
			parse = Rio.parse(getResourceAsStream("bsbm-100.ttl"), "", RDFFormat.TURTLE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testStaleStats() throws InterruptedException {

		ExtensibleDynamicEvaluationStatistics extensibleDynamicEvaluationStatistics = new ExtensibleDynamicEvaluationStatistics(
				null);

		parse.forEach(s -> extensibleDynamicEvaluationStatistics.add(ex.fromStatement(s, false)));
		extensibleDynamicEvaluationStatistics.waitForQueue();
		double staleness1 = extensibleDynamicEvaluationStatistics.staleness(parse.size());
		roundedAssert(0, staleness1);

		parse.forEach(s -> extensibleDynamicEvaluationStatistics.remove(ex.fromStatement(s, false)));
		extensibleDynamicEvaluationStatistics.waitForQueue();
		double staleness2 = extensibleDynamicEvaluationStatistics.staleness(0);
		roundedAssert(0, staleness2);

		IntStream.range(0, 100).forEach(i -> {
			extensibleDynamicEvaluationStatistics
					.add(ex.fromStatement(vf.createStatement(RDF.TYPE, RDFS.LABEL, vf.createLiteral(i + "a")), false));
		});
		parse.forEach(s -> extensibleDynamicEvaluationStatistics.add(ex.fromStatement(s, false)));
		extensibleDynamicEvaluationStatistics.waitForQueue();

		double staleness3 = extensibleDynamicEvaluationStatistics.staleness(100 + parse.size());
		roundedAssert(1, staleness3);

		IntStream.range(0, 100000).forEach(i -> {
			extensibleDynamicEvaluationStatistics
					.add(ex.fromStatement(vf.createStatement(RDF.TYPE, RDFS.LABEL, vf.createLiteral(i + "b")), false));
		});

		extensibleDynamicEvaluationStatistics.waitForQueue();

		double staleness4 = extensibleDynamicEvaluationStatistics.staleness(100000 + 100 + parse.size());

		roundedAssert(0.3, staleness4);

	}

	@Test
	public void stalenessCalculationTest() throws InterruptedException {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		ExtensibleDynamicEvaluationStatistics extensibleDynamicEvaluationStatistics = new ExtensibleDynamicEvaluationStatistics(
				null);

		parse.forEach(s -> extensibleDynamicEvaluationStatistics.add(ex.fromStatement(s, false)));
		extensibleDynamicEvaluationStatistics.waitForQueue();

		double staleness1 = extensibleDynamicEvaluationStatistics.staleness(parse.size());
		roundedAssert(0, staleness1);

		double staleness2 = extensibleDynamicEvaluationStatistics.staleness(parse.size() * 3);
		roundedAssert(0.7, staleness2);

		double staleness3 = extensibleDynamicEvaluationStatistics.staleness(parse.size() / 3);
		roundedAssert(0.7, staleness3);

	}

	@Test
	public void testAcurracy() throws InterruptedException {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		ExtensibleDynamicEvaluationStatistics extensibleDynamicEvaluationStatistics = new ExtensibleDynamicEvaluationStatistics(
				null);

		parse.forEach(s -> extensibleDynamicEvaluationStatistics.add(ex.fromStatement(s, false)));
		extensibleDynamicEvaluationStatistics.waitForQueue();

		ExtensibleDynamicEvaluationStatistics.ExtensibleDynamicEvaluationStatisticsCardinalityCalculator cardinalityCalculator = (ExtensibleDynamicEvaluationStatistics.ExtensibleDynamicEvaluationStatisticsCardinalityCalculator) extensibleDynamicEvaluationStatistics
				.createCardinalityCalculator();

		IRI bdbmProductType = vf.createIRI("http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/", "ProductType");
		IRI dataFromProducer1Product31 = vf
				.createIRI("http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromProducer1/", "Product31");

		StatementPattern null_rdfType_bsbmProductType = new StatementPattern(
				new Var("a", null),
				new Var("b", RDF.TYPE),
				new Var("c", bdbmProductType));

		checkPattern(cardinalityCalculator, null_rdfType_bsbmProductType, 1);

		StatementPattern null_null_null = new StatementPattern(
				new Var("a", null),
				new Var("b", null),
				new Var("c", null));

		checkPattern(cardinalityCalculator, null_null_null, 1);

		StatementPattern null_rdfType_null = new StatementPattern(
				new Var("a", null),
				new Var("b", RDF.TYPE),
				new Var("c", null));

		checkPattern(cardinalityCalculator, null_rdfType_null, 1);

		StatementPattern nonExistent = new StatementPattern(
				new Var("a", null),
				new Var("b", vf.createIRI("http://example.com/fhjerhf2uhfjkdsbf32o")),
				new Var("c", null));

		checkPattern(cardinalityCalculator, nonExistent, 1);

		// this last pattern isn't very accurate, it's actually 46 statements, but the estimate is 100.4
		StatementPattern bsbmProductType_null_null = new StatementPattern(
				new Var("a", dataFromProducer1Product31),
				new Var("b", null),
				new Var("c", null));

		checkPattern(cardinalityCalculator, bsbmProductType_null_null, 120);

	}

	private void checkPattern(
			ExtensibleDynamicEvaluationStatistics.ExtensibleDynamicEvaluationStatisticsCardinalityCalculator cardinalityCalculator,
			StatementPattern pattern, int percentage) {
		double estimatedCardinality = cardinalityCalculator.getCardinality(pattern);
		int actual = parse
				.filter((Resource) pattern.getSubjectVar().getValue(), (IRI) pattern.getPredicateVar().getValue(),
						pattern.getObjectVar().getValue())
				.size();

		fuzzyAssert(actual, estimatedCardinality, percentage);
	}

	private void fuzzyAssert(int expected, double estimated, int percentage) {

		double diff = Math.abs(expected - estimated);
		if (100.0 / expected * diff > percentage) {
			assertEquals("Estimated cardinality should be within " + percentage + "% of expected cardinality", expected,
					estimated);
		}

	}

	private void roundedAssert(double expected, double actual) {
		assertEquals(expected, Math.round(actual * 10) / 10.0);
	}

	private static InputStream getResourceAsStream(String name) {
		return EvaluationStatisticsTest.class.getClassLoader().getResourceAsStream(name);
	}

}
