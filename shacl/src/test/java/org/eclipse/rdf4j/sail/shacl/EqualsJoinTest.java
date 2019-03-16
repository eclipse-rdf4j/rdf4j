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
import org.eclipse.rdf4j.sail.shacl.planNodes.EqualsJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Håvard Ottestad
 */
public class EqualsJoinTest {

	{
		LoggingNode.loggingEnabled = true;
	}

	@Test
	public void testSimple01() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("b"));

		EqualsJoin equalsJoin = new EqualsJoin(left, right, false);

		List<Tuple> tuples = new MockConsumePlanNode(equalsJoin).asList();

		verify(tuples, Arrays.asList("a"));

	}

	@Test
	public void testSimple02() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("c"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c"));

		EqualsJoin equalsJoin = new EqualsJoin(left, right, false);

		List<Tuple> tuples = new MockConsumePlanNode(equalsJoin).asList();

		verify(tuples, Arrays.asList("a"), Arrays.asList("c"));

	}

	@Test
	public void testSimple03() {

		PlanNode right = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("c"));
		PlanNode left = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c"));

		EqualsJoin equalsJoin = new EqualsJoin(left, right, false);

		List<Tuple> tuples = new MockConsumePlanNode(equalsJoin).asList();

		verify(tuples, Arrays.asList("a"), Arrays.asList("c"));

	}

	@Test
	public void testSimple04() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("b"), Arrays.asList("c"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("d"), Arrays.asList("e"));

		EqualsJoin equalsJoin = new EqualsJoin(left, right, false);

		List<Tuple> tuples = new MockConsumePlanNode(equalsJoin).asList();

		verify(tuples);

	}

	@Test
	public void testSimple05() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("b"), Arrays.asList("c"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("d"), Arrays.asList("c"));

		EqualsJoin equalsJoin = new EqualsJoin(left, right, false);

		List<Tuple> tuples = new MockConsumePlanNode(equalsJoin).asList();

		verify(tuples, Arrays.asList("c"));

	}

	@Test
	public void testSimple06() {

		PlanNode left = new MockInputPlanNode(Arrays.asList("a", "1"), Arrays.asList("b", "1"), Arrays.asList("b", "1"),
				Arrays.asList("c", "1"));
		PlanNode right = new MockInputPlanNode(Arrays.asList("a", "1"), Arrays.asList("b", "1"),
				Arrays.asList("b", "1"), Arrays.asList("c", "1"));

		EqualsJoin equalsJoin = new EqualsJoin(left, right, true);

		List<Tuple> tuples = new MockConsumePlanNode(equalsJoin).asList();

		verify(tuples, Arrays.asList("a", "1"), Arrays.asList("b", "1"), Arrays.asList("c", "1"));

	}

	public void verify(List<Tuple> actual, List<String>... expect) {

		System.out.println(actual);

		Set<Tuple> collect = Arrays.stream(expect)
				.map(strings -> strings.stream()
						.map(SimpleValueFactory.getInstance()::createLiteral)
						.map(l -> (Value) l)
						.collect(Collectors.toList()))
				.map(Tuple::new)
				.collect(Collectors.toSet());

		Set<Tuple> actualSet = new HashSet<>(actual);

		assertEquals(collect, actualSet);

		assertTrue(collect.containsAll(actualSet));
		assertTrue(actualSet.containsAll(collect));

	}

}
