/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetClass;
import org.junit.jupiter.api.Test;

public class StableQueryGenerationTest {

	@Test
	public void testTargetClass() {

		List<IRI> collect = Arrays.asList(Values.iri("http://b"), Values.iri("http://a"), Values.iri("http://c"));

		TargetClass targetClass1 = new TargetClass(new HashSet<>(collect));

		TreeSet<Resource> treeSet = new TreeSet<>(new ValueComparator());
		treeSet.addAll(collect);
		TargetClass targetClass2 = new TargetClass(treeSet);

		String targetQueryFragment1 = targetClass1.getTargetQueryFragment(null, new StatementMatcher.Variable("b"),
				null, new StatementMatcher.StableRandomVariableProvider());
		String targetQueryFragment2 = targetClass2.getTargetQueryFragment(null, new StatementMatcher.Variable("b"),
				null, new StatementMatcher.StableRandomVariableProvider());

		assertEquals(targetQueryFragment1, targetQueryFragment2);

		String queryFragment1 = targetClass1.getQueryFragment("a", "b", null,
				new StatementMatcher.StableRandomVariableProvider());
		String queryFragment2 = targetClass2.getQueryFragment("a", "b", null,
				new StatementMatcher.StableRandomVariableProvider());

		assertEquals(queryFragment1, queryFragment2);

	}

	@Test
	public void testNormalizationOfQuery() {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();
		String[] names = {
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
				stableRandomVariableProvider.next().getName(),
		};

		String test1 = names[4] + names[4] + names[9];
		assertEquals(names[0] + names[0] + names[1], StatementMatcher.StableRandomVariableProvider.normalize(test1));

		String test2 = names[9] + names[4] + names[9];
		assertEquals(names[1] + names[0] + names[1], StatementMatcher.StableRandomVariableProvider.normalize(test2));

		String test3 = names[0] + names[1] + names[2];
		assertEquals(names[0] + names[1] + names[2], StatementMatcher.StableRandomVariableProvider.normalize(test3));

		String test4 = names[1] + names[2] + names[3];
		assertEquals(names[0] + names[1] + names[2], StatementMatcher.StableRandomVariableProvider.normalize(test4));

		String test5 = names[2] + names[4] + names[8];
		assertEquals(names[0] + names[1] + names[2], StatementMatcher.StableRandomVariableProvider.normalize(test5));

		String test6 = names[8] + names[4] + names[2];
		assertEquals(names[2] + names[1] + names[0], StatementMatcher.StableRandomVariableProvider.normalize(test6));

	}

}
