/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for BindingSet compatibility API. Verifies that BindingSet#isCompatible has identical semantics to
 * QueryResults#bindingSetsCompatible across a variety of scenarios.
 */
public class BindingSetCompatibleTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Test
	public void isCompatible_exists_and_matches_QueryResults() throws Exception {
		// Prefer the current API name; fallback to legacy name if present
		Method m;
		try {
			m = BindingSet.class.getMethod("isCompatible", BindingSet.class);
		} catch (NoSuchMethodException e1) {
			try {
				m = BindingSet.class.getMethod("bindingSetCompatible", BindingSet.class);
			} catch (NoSuchMethodException e2) {
				fail("BindingSet#isCompatible(BindingSet) method is missing");
				return;
			}
		}

		// Verify semantics align with QueryResults.bindingSetsCompatible on a few basic cases
		List<String> names = Arrays.asList("a", "b");

		BindingSet s1 = new ListBindingSet(names, VF.createIRI("urn:x"), VF.createLiteral(1));
		BindingSet s2 = new ListBindingSet(names, VF.createIRI("urn:x"), VF.createLiteral(2));
		boolean expected = QueryResults.bindingSetsCompatible(s1, s2);
		boolean actual = (Boolean) m.invoke(s1, s2);
		assertThat(actual).isEqualTo(expected);

		BindingSet s3 = new ListBindingSet(names, VF.createIRI("urn:x"), VF.createLiteral(1));
		BindingSet s4 = new ListBindingSet(names, VF.createIRI("urn:x"), VF.createLiteral(1));
		expected = QueryResults.bindingSetsCompatible(s3, s4);
		actual = (Boolean) m.invoke(s3, s4);
		assertThat(actual).isEqualTo(expected);

		BindingSet s5 = new ListBindingSet(names, null, VF.createLiteral(1));
		BindingSet s6 = new ListBindingSet(names, null, VF.createLiteral(2));
		expected = QueryResults.bindingSetsCompatible(s5, s6);
		actual = (Boolean) m.invoke(s5, s6);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void isCompatible_empty_sets_true() {
		BindingSet empty1 = new ListBindingSet(List.of());
		BindingSet empty2 = new ListBindingSet(List.of());
		assertThat(empty1.isCompatible(empty2)).isTrue();
		assertThat(empty2.isCompatible(empty1)).isTrue();
	}

	@Test
	public void isCompatible_one_empty_true() {
		List<String> names = Arrays.asList("a", "b");
		BindingSet nonEmpty = new ListBindingSet(names, VF.createIRI("urn:x"), VF.createLiteral(1));
		BindingSet empty = new ListBindingSet(List.of());
		assertThat(nonEmpty.isCompatible(empty)).isTrue();
		assertThat(empty.isCompatible(nonEmpty)).isTrue();
	}

	@Test
	public void isCompatible_disjoint_names_true() {
		BindingSet aOnly = new ListBindingSet(List.of("a"), VF.createLiteral(1));
		BindingSet bOnly = new ListBindingSet(List.of("b"), VF.createLiteral(2));
		assertThat(aOnly.isCompatible(bOnly)).isTrue();
		assertThat(bOnly.isCompatible(aOnly)).isTrue();
	}

	@Test
	public void isCompatible_partial_overlap_true_when_equal() {
		List<String> namesAB = Arrays.asList("a", "b");
		BindingSet ab = new ListBindingSet(namesAB, VF.createIRI("urn:x"), VF.createLiteral(1));
		BindingSet b = new ListBindingSet(List.of("b"), VF.createLiteral(1));
		assertThat(ab.isCompatible(b)).isTrue();
		assertThat(b.isCompatible(ab)).isTrue();
	}

	@Test
	public void isCompatible_partial_overlap_false_when_conflict() {
		List<String> namesAB = Arrays.asList("a", "b");
		BindingSet ab = new ListBindingSet(namesAB, VF.createIRI("urn:x"), VF.createLiteral(1));
		BindingSet bConflict = new ListBindingSet(List.of("b"), VF.createLiteral(2));
		assertThat(ab.isCompatible(bConflict)).isFalse();
		assertThat(bConflict.isCompatible(ab)).isFalse();
	}

	@Test
	public void isCompatible_null_variable_ignored_when_overlap_equal() {
		List<String> namesAB = Arrays.asList("a", "b");
		BindingSet s1 = new ListBindingSet(namesAB, null, VF.createLiteral(1));
		BindingSet s2 = new ListBindingSet(namesAB, null, VF.createLiteral(1));
		assertThat(s1.isCompatible(s2)).isTrue();
		assertThat(s2.isCompatible(s1)).isTrue();
	}

	@ParameterizedTest(name = "fuzz case {index}")
	@MethodSource("fuzzCases")
	public void isCompatible_fuzz_parity_and_symmetry(BindingSet s1, BindingSet s2) {
		boolean expected = QueryResults.bindingSetsCompatible(s1, s2);
		assertThat(s1.isCompatible(s2)).isEqualTo(expected);
		// symmetry
		boolean expectedReverse = QueryResults.bindingSetsCompatible(s2, s1);
		assertThat(s2.isCompatible(s1)).isEqualTo(expectedReverse);
	}

	static Stream<Arguments> fuzzCases() {
		Random rnd = new Random(424242);
		List<String> universe = Arrays.asList("a", "b", "c", "d", "e", "x", "y", "z");
		int cases = 128; // balanced coverage and speed
		Stream.Builder<Arguments> b = Stream.builder();
		for (int i = 0; i < cases; i++) {
			BindingSet s1 = randomBindingSet(rnd, universe);
			BindingSet s2 = randomBindingSet(rnd, universe);
			b.add(Arguments.of(s1, s2));
		}
		return b.build();
	}

	private static BindingSet randomBindingSet(Random rnd, List<String> universe) {
		// Randomly decide how many variables to include (possibly zero)
		int n = rnd.nextInt(universe.size() + 1); // 0..universe.size()
		// Shuffle-like selection via random threshold
		java.util.ArrayList<String> selected = new java.util.ArrayList<>(universe.size());
		for (String name : universe) {
			if (selected.size() >= n) {
				break;
			}
			// ~50% chance to include each name until we reach n
			if (rnd.nextBoolean()) {
				selected.add(name);
			}
		}
		// If selection under-filled, top up from remaining deterministically
		for (String name : universe) {
			if (selected.size() >= n) {
				break;
			}
			if (!selected.contains(name)) {
				selected.add(name);
			}
		}

		Value[] values = new Value[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			values[i] = randomValueOrNull(rnd, i);
		}
		return new ListBindingSet(selected, values);
	}

	private static org.eclipse.rdf4j.model.Value randomValueOrNull(Random rnd, int salt) {
		int pick = rnd.nextInt(6);
		switch (pick) {
		case 0:
			return null; // unbound
		case 1:
			return VF.createIRI("urn:res:" + rnd.nextInt(10));
		case 2:
			return VF.createLiteral(rnd.nextInt(5));
		case 3:
			return VF.createLiteral("s" + rnd.nextInt(5));
		case 4:
			return VF.createBNode("b" + rnd.nextInt(5));
		default:
			return VF.createIRI("urn:x:" + ((salt + rnd.nextInt(5)) % 7));
		}
	}

}
