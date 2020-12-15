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

		runTest(input);

	}

	@Test
	public void tupleCardinality2() {

		MockInputPlanNode input = new MockInputPlanNode(Arrays.asList("a"), Arrays.asList("b", "2"), Arrays.asList("b"),
				Arrays.asList("b", "3"), Arrays.asList("b", "2"), Arrays.asList("c", "1"));

		runTest(input);

	}

	@Test
	public void tupleCardinality3() {

		MockInputPlanNode input = new MockInputPlanNode(Arrays.asList("a", "1"), Arrays.asList("a", "1"),
				Arrays.asList("a", "1"), Arrays.asList("a", "1"));

		runTest(input);

	}

	private void runTest(MockInputPlanNode input) {
		Unique unique = new Unique(input);

		List<ValidationTuple> tuples = new MockConsumePlanNode(unique).asList();

		ArrayList<ValidationTuple> expected = new ArrayList<>(new HashSet<>(new MockConsumePlanNode(input).asList()));

		tuples.sort(ValidationTuple::compareTarget);
		expected.sort(ValidationTuple::compareTarget);

		assertEquals(expected, tuples);
	}

}
