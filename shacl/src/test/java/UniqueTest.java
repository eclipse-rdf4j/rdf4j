
/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

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

	private void runTest(MockInputPlanNode input) {
		Unique unique = new Unique(input);

		List<Tuple> tuples = new MockConsumePlanNode(unique).asList();

		ArrayList<Tuple> expected = new ArrayList<>(new HashSet<>(new MockConsumePlanNode(input).asList()));

		tuples.sort(Tuple::compareTo);
		expected.sort(Tuple::compareTo);

		assertEquals(expected, tuples);
	}

}
