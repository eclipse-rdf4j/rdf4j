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

package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for SPARQLMinusIteration to ensure semantics match SPARQL 1.1 MINUS.
 */
public class SPARQLMinusIterationTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private static CloseableIteration<BindingSet> iter(List<BindingSet> list) {
		return new CloseableIteratorIteration<>(list.iterator());
	}

	private static QueryBindingSet qbs(Object... nv) {
		QueryBindingSet q = new QueryBindingSet();
		for (int i = 0; i + 1 < nv.length; i += 2) {
			String name = (String) nv[i];
			Object val = nv[i + 1];
			if (val == null) {
				q.setBinding(name, null);
			} else if (val instanceof Integer) {
				q.setBinding(name, VF.createLiteral((Integer) val));
			} else if (val instanceof String && ((String) val).startsWith("urn:")) {
				q.setBinding(name, VF.createIRI((String) val));
			} else {
				q.setBinding(name, VF.createLiteral(String.valueOf(val)));
			}
		}
		return q;
	}

	private static ArrayBindingSet abs(String[] names, Object... nv) {
		ArrayBindingSet a = new ArrayBindingSet(names);
		for (int i = 0; i + 1 < nv.length; i += 2) {
			String name = (String) nv[i];
			Object val = nv[i + 1];
			if (val == null) {
				a.setBinding(name, null);
			} else if (val instanceof Integer) {
				a.setBinding(name, VF.createLiteral((Integer) val));
			} else if (val instanceof String && ((String) val).startsWith("urn:")) {
				a.setBinding(name, VF.createIRI((String) val));
			} else {
				a.setBinding(name, VF.createLiteral(String.valueOf(val)));
			}
		}
		return a;
	}

	private static Set<String> runAndCollectIds(SPARQLMinusIteration it, String idVar) {
		Set<String> ids = new HashSet<>();
		while (it.hasNext()) {
			BindingSet bs = it.next();
			var v = bs.getValue(idVar);
			ids.add(v == null ? "<null>" : v.stringValue());
		}
		return ids;
	}

	@Test
	public void emptyRight_acceptAllLeft() {
		List<BindingSet> left = Arrays.asList(
				qbs("id", "urn:L1", "a", 1),
				qbs("id", "urn:L2")
		);
		List<BindingSet> right = List.of();
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L1", "urn:L2");
	}

	@Test
	public void emptyLeft_yieldsEmpty() {
		List<BindingSet> left = List.of();
		List<BindingSet> right = Arrays.asList(qbs("x", 1));
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).isEmpty();
	}

	@Test
	public void noSharedVariables_acceptAll() {
		List<BindingSet> left = Arrays.asList(
				qbs("id", "urn:L1", "a", 1),
				qbs("id", "urn:L2", "b", 2)
		);
		List<BindingSet> right = Arrays.asList(
				qbs("x", "urn:R1"),
				qbs("y", 3)
		);
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L1", "urn:L2");
	}

	@Test
	public void sharedVar_conflictingValues_accept() {
		List<BindingSet> left = Arrays.asList(
				qbs("id", "urn:L1", "a", 1),
				qbs("id", "urn:L2", "a", 2)
		);
		List<BindingSet> right = Arrays.asList(qbs("a", 3));
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L1", "urn:L2");
	}

	@Test
	public void sharedVar_matchingValue_reject() {
		List<BindingSet> left = Arrays.asList(
				qbs("id", "urn:L1", "a", 1),
				qbs("id", "urn:L2", "a", 2)
		);
		List<BindingSet> right = Arrays.asList(qbs("a", 2));
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L1");
	}

	@Test
	public void multipleSharedVars_requireAllMatchToReject() {
		List<BindingSet> left = Arrays.asList(
				qbs("id", "urn:L1", "a", 1, "b", 2),
				qbs("id", "urn:L2", "a", 1, "b", 3)
		);
		List<BindingSet> right = Arrays.asList(
				qbs("a", 1, "b", 2), // should reject L1
				qbs("a", 1, "b", 9) // does not reject L2 (b differs)
		);
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L2");
	}

	@Test
	public void leftNullValue_treatedAsUnbound_notRejected() {
		List<BindingSet> left = Arrays.asList(
				qbs("id", "urn:L1", "a", null), // a is set but null
				qbs("id", "urn:L2", "a", 2)
		);
		List<BindingSet> right = Arrays.asList(qbs("a", 2));
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L1");
	}

	@Test
	public void arrayBindingSet_compatibleAndDisjoint() {
		String[] names = new String[] { "id", "a", "b" };
		List<BindingSet> left = Arrays.asList(
				abs(names, "id", "urn:L1", "a", 1, "b", 2),
				abs(names, "id", "urn:L2", "a", 5)
		);
		List<BindingSet> right = Arrays.asList(
				abs(names, "a", 1), // overlaps a=1 => reject L1
				abs(names, "x", 9) // disjoint with both => should not reject
		);
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactlyInAnyOrder("urn:L2");
	}

	@Test
	public void unionNoOverlap_acceptFastPath() {
		// Right union names = {a,b}; left rows only have {z}
		List<BindingSet> left = Arrays.asList(qbs("id", "urn:L1", "z", 1));
		List<BindingSet> right = Arrays.asList(qbs("a", 1), qbs("b", 2));
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactly("urn:L1");
	}

	@Test
	public void duplicateRightRows_doNotChangeResult() {
		List<BindingSet> left = Arrays.asList(qbs("id", "urn:L1", "a", 1), qbs("id", "urn:L2", "a", 2));
		List<BindingSet> right = Arrays.asList(qbs("a", 2), qbs("a", 2));
		SPARQLMinusIteration it = new SPARQLMinusIteration(iter(left), iter(right));
		assertThat(runAndCollectIds(it, "id")).containsExactly("urn:L1");
	}
}
