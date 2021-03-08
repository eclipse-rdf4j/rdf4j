/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
import org.junit.Test;

public class UniqueTest {

	@Test
	public void tupleCardinality1() {

		MockInputPlanNode input = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("b"),
				Arrays.asList("c"));

		runTest(input, false);

	}

	@Test
	public void tupleCardinality2() {

		MockInputPlanNode input = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("b", "2"), Arrays.asList("b"),
				Arrays.asList("b", "3"), Arrays.asList("b", "2"), Arrays.asList("c", "1"));

		runTest(input, false);

	}

	@Test
	public void tupleCardinality3() {

		MockInputPlanNode input = new MockInputPlanNode(Arrays.asList("a", "1"), Arrays.asList("a", "1"),
				Arrays.asList("a", "1"), Arrays.asList("a", "1"));

		runTest(input, false);

	}

	@Test
	public void compressPropertyShape() {

		MockInputPlanNode input = new MockInputPlanNode(
				Arrays.asList("a", "1"),
				Arrays.asList("b", "1"),
				Arrays.asList("c", "1"),
				Arrays.asList("d", "1"),
				Arrays.asList("a", "2")
		);

		runTest(input, true);

	}

	@Test
	public void compressPropertyShape2() {

		MockInputPlanNode input = new MockInputPlanNode(
				Arrays.asList("a", "a", "1"),
				Arrays.asList("b", "a", "1"),
				Arrays.asList("c", "a", "1"),
				Arrays.asList("d", "a", "1"),
				Arrays.asList("a", "a", "2")
		);

		Unique unique = new Unique(input, true);

		List<ValidationTuple> tuples = new MockConsumePlanNode(unique).asList();

		assertEquals(2, tuples.size());

	}

	private void runTest(MockInputPlanNode input, boolean compress) {
		Unique unique = new Unique(input, compress);

		List<ValidationTuple> tuples = new MockConsumePlanNode(unique).asList();

		ArrayList<ValidationTuple> expected = new ArrayList<>(new HashSet<>(new MockConsumePlanNode(input).asList()));

		tuples.sort(ValidationTuple::compareValue);
		expected.sort(ValidationTuple::compareValue);

		tuples.sort(ValidationTuple::compareFullTarget);
		expected.sort(ValidationTuple::compareFullTarget);

		assertEquals(expected, tuples);
	}

}
