/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.federation.optimizers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class CardinalityTest {

	protected static StatementPattern statement;
	protected static double cardinality;
	protected static EvaluationStatistics.CardinalityCalculator calculator;
	protected static SimpleValueFactory factory;
	protected static String namespace;

	@Before
	public void setup() {
		factory = SimpleValueFactory.getInstance();
		namespace = "ex:";
		EvaluationStatistics evaluationStatistics = new EvaluationStatistics();
		calculator = evaluationStatistics.createCardinalityCalculator();
	}

	@Test
	public void assertDuplicateElementsCardinality() {
		IRI subjectIRI = factory.createIRI(namespace, "Set1");
		IRI objectIRI = factory.createIRI(namespace, "Set1");
		Var subject = new Var("Set1", subjectIRI);
		Var predicate = new Var("subsetOf", null);
		Var object = new Var("Set1", objectIRI);

		statement = new StatementPattern(subject, predicate, object);
		calculator.meet(statement);
		cardinality = calculator.getCardinality();

		assertEquals(31.623, cardinality, 0.001);
	}

	@Test
	public void assertDistinctElementsCardinality() {
		IRI subjectIRI = factory.createIRI(namespace, "Set1");
		IRI objectIRI = factory.createIRI(namespace, "Set2");
		Var subject = new Var("Set1", subjectIRI);
		Var predicate = new Var("subsetOf", null);
		Var object = new Var("Set2", objectIRI);

		statement = new StatementPattern(subject, predicate, object);
		calculator.meet(statement);
		cardinality = calculator.getCardinality();

		assertEquals(10.0, cardinality, 0.000001);
	}

}
