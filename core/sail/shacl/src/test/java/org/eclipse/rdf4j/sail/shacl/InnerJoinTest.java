/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class InnerJoinTest {

	@BeforeClass
	public static void beforeClass() {
		// GlobalValidationExecutionLogging.loggingEnabled = true;

	}

	@AfterClass
	public static void afterClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void testSimple() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a", "b"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a", "b"));

	}

	@Test
	public void testSimple2() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b"));

	}

	@Test
	public void testSimple3() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a2", "b"));

	}

	@Test
	public void testSimple4() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList());

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples);

	}

	@Test
	public void testSimple5() {

		PlanNode left = new MockInputPlanNode();
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples);

	}

	@Test
	public void testSimple6() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
				Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"),
				Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"),
				Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple7() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"));

	}

	@Test
	public void testSimple8() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"),
				Arrays.asList("a3", "b3"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"));

	}

	@Test
	public void testSimple9() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
				Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"),
				Arrays.asList("a2", "b22"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"),
				Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple10() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
				Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"),
				Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"),
				Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple11() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
				Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"),
				Arrays.asList("a4", "b4"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));

	}

	@Test
	public void testSimple12() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
				Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

	}

	@Test
	public void testSimple13() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
				Arrays.asList("a4"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a1", "b11"),
				Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));

		PlanNode innerJoin = new InnerJoin(left, right).getJoined(BufferedPlanNode.class);

		List<ValidationTuple> tuples = new MockConsumePlanNode(innerJoin).asList();

//		tuples.forEach(System.out::println);

		verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a1", "b11"), Arrays.asList("a2", "b2"),
				Arrays.asList("a2", "b22"));

	}

	@SafeVarargs
	private final void verify(List<ValidationTuple> actual, List<String>... expect) {

		Set<ValidationTuple> collect = Arrays.stream(expect)
				.map(strings -> strings.stream()
						.map(SimpleValueFactory.getInstance()::createLiteral)
						.map(l -> (Value) l)
						.collect(Collectors.toList()))
				.map(v -> {
					assert (v.size() == 2);
					return new ValidationTuple(v, ConstraintComponent.Scope.propertyShape, true);
				})
				.collect(Collectors.toSet());

		Set<ValidationTuple> actualSet = new HashSet<>(actual);

		assertTrue(collect.containsAll(actualSet));
		assertTrue(actualSet.containsAll(collect));

	}

}
