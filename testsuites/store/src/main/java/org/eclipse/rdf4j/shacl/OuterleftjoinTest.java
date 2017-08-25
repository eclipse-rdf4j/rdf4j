/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.plan.OuterLeftJoin;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Tuple;
import org.eclipse.rdf4j.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.shacl.mock.MockInputPlanNode;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class OuterleftjoinTest {

	@Test
	public void simpleJoinCase1(){
		Object[][] targetclass = {{"Heshan", FOAF.PERSON}};
		String[][] properties = { {"Heshan","he"}};


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person, http://example.org/Heshan, \"he\"^^<http://www.w3.org/2001/XMLSchema#string>]}", s);
	}


	@Test
	public void simpleJoinCase2(){
		Object[][] targetclass = {{"Heshan", FOAF.PERSON}};
		String[][] properties = { {"Heshan","he"}, {"Heshan","he2"}};


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person, http://example.org/Heshan, \"he\"^^<http://www.w3.org/2001/XMLSchema#string>]}\n" +
			"Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person, http://example.org/Heshan, \"he2\"^^<http://www.w3.org/2001/XMLSchema#string>]}", s);

	}

	@Test
	public void simpleJoinCase3(){
		Object[][] targetclass = {{"Heshan", FOAF.PERSON}, {"Håvard", FOAF.PERSON}};
		String[][] properties = { {"Heshan","he"},{"Håvard","hå"},  {"Heshan","he2"}};


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person, http://example.org/Heshan, \"he\"^^<http://www.w3.org/2001/XMLSchema#string>]}\n" +
			"Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person, http://example.org/Heshan, \"he2\"^^<http://www.w3.org/2001/XMLSchema#string>]}\n" +
			"Tuple{line=[http://example.org/Håvard, http://xmlns.com/foaf/0.1/Person, http://example.org/Håvard, \"hå\"^^<http://www.w3.org/2001/XMLSchema#string>]}", s);

	}

	@Test
	public void simpleEmptyCase1(){
		Object[][] targetclass = {};
		String[][] properties = { {"Heshan","he"},{"Håvard","hå"},  {"Heshan","he2"}};


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("", s);

	}

	@Test
	public void simpleLeftJoinEmptyCase1(){
		Object[][] targetclass = {{"Heshan", FOAF.PERSON}};
		String[][] properties = { };


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person]}", s);

	}

	@Test
	public void simpleLeftJoinEmptyCase2(){
		Object[][] targetclass = {{"Heshan", FOAF.PERSON}, {"Håvard", FOAF.PERSON}};
		String[][] properties = { };


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("Tuple{line=[http://example.org/Heshan, http://xmlns.com/foaf/0.1/Person]}\n" +
			"Tuple{line=[http://example.org/Håvard, http://xmlns.com/foaf/0.1/Person]}", s);

	}


	@Test
	public void simpleEmptyCase3(){
		Object[][] targetclass = {};
		String[][] properties = { };


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("", s);

	}

	@Test
	public void testOuterleftjoin() {

		Object[][] targetclass = {{"Rebecca", FOAF.PERSON}, {"Håvard", FOAF.PERSON}, {"Heshan", FOAF.PERSON}, {"Anna", FOAF.PERSON}, {"Peter", FOAF.PERSON}, {"Håvard", FOAF.AGENT}, {"Lucy", FOAF.PERSON}};
		String[][] properties = { {"Håvard","hå"}, {"Peter","p"}, {"Håvard","hå2"}, {"Peter","p2"},{"Heshan","he"},{"Heshan","he2"},{"Peter","p3"}};


		String s = join(targetclass, properties);
		System.out.println(s);
		assertEquals("", s);



	}

	private String join(Object[][] targetclass, String[][] properties) {
		SimpleValueFactory simpleValueFactory = SimpleValueFactory.getInstance();

		List<Tuple> inputDataLeft = Arrays
			.stream(targetclass)
			.map(person -> {
				IRI iri = simpleValueFactory.createIRI("http://example.org/" + person[0]);
				return new Tuple(Arrays.asList(iri, (Value) person[1]));
			})
			.collect(Collectors.toList());


		PlanNode leftNode = new MockInputPlanNode(inputDataLeft);


		List<Tuple> inputDataRight = Arrays
			.stream(properties)
			.map(person -> {
				IRI iri = simpleValueFactory.createIRI("http://example.org/" + person[0]);
				return new Tuple(Arrays.asList(iri, simpleValueFactory.createLiteral(person[1])));
			})
			.collect(Collectors.toList());


		MockInputPlanNode rightNode = new MockInputPlanNode(inputDataRight);


		OuterLeftJoin outerLeftJoin = new OuterLeftJoin(leftNode, rightNode);

		List<Tuple> joinedTuples = new MockConsumePlanNode(outerLeftJoin).asList();


		Optional<String> reduce = joinedTuples.stream().map(Object::toString).reduce((l, r) -> l + "\n" + r);


		return reduce.orElse("");
	}
}