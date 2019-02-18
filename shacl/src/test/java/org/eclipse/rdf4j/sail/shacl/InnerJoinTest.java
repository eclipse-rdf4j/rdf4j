/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Håvard Ottestad
 */
public class InnerJoinTest {

	{
		LoggingNode.loggingEnabled = true;
	}

	@Test
	public void testSimple() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a", "b"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a", "b"));

	}


	@Test
	public void testSimple2() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b"));

	}


	@Test
	public void testSimple3() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a2", "b"));

	}


	@Test
	public void testSimple4() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList());

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples);

	}


	@Test
	public void testSimple5() {


		PlanNode left = new MockInputPlanNode();
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples);

	}

	@Test
	public void testSimple6() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"), Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple7() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"));

	}

	@Test
	public void testSimple8() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"));

		PlanNode innerJoin = new LoggingNode(new InnerJoin(left, right, null, null), "");

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"));

	}

	@Test
	public void testSimple9() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"), Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple10() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"), Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));

	}


	@Test
	public void testSimple11() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"), Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple12() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"), Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

	}


	@Test
	public void testSimple13() {


		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"), Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a1", "b11"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

		InnerJoin innerJoin = new InnerJoin(left, right, null, null);

		List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();

		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a1", "b11"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

	}




	public void verify(List<Tuple> actual, List<String>... expect) {


		Set<Tuple> collect = Arrays
			.stream(expect)
			.map(strings -> strings.stream().map(SimpleValueFactory.getInstance()::createLiteral).map(l -> (Value) l).collect(Collectors.toList()))
			.map(Tuple::new)
			.collect(Collectors.toSet());

		Set<Tuple> actualSet = new HashSet<>(actual);

		assertTrue(collect.containsAll(actualSet));
		assertTrue(actualSet.containsAll(collect));

	}


}
