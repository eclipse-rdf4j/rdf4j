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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.junit.jupiter.api.Test;

/**
 * Randomized fuzz tests validating SPARQLMinusIteration against a reference MINUS implementation.
 */
public class SPARQLMinusIterationFuzzTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final String[] UNIVERSE = new String[] { "a", "b", "c", "d", "e", "x", "y", "z", "id" };

	@Test
	public void randomizedMinusParity() {
		long[] seeds = new long[] { 42L, 4242L, 424242L, 7L, 123456789L };
		for (long seed : seeds) {
			Random rnd = new Random(seed);
			int leftSize = rnd.nextInt(15) + 5; // 5..19
			int rightSize = rnd.nextInt(15) + 5; // 5..19

			List<BindingSet> left = new ArrayList<>(leftSize);
			List<BindingSet> right = new ArrayList<>(rightSize);

			for (int i = 0; i < leftSize; i++) {
				left.add(randomBindingSet(rnd, i));
			}
			for (int i = 0; i < rightSize; i++) {
				right.add(randomBindingSet(rnd, i + 1000));
			}

			Set<String> actual = collect(replay(new SPARQLMinusIteration(iter(left), iter(right))));
			Set<String> expected = collect(mapToKeys(referenceMinus(left, right)));

			assertThat(actual).as("minus parity for seed=" + seed).isEqualTo(expected);
		}
	}

	private static CloseableIteration<BindingSet> iter(List<BindingSet> list) {
		return new CloseableIteratorIteration<>(list.iterator());
	}

	private static BindingSet randomBindingSet(Random rnd, int salt) {
		// Random subset of variables
		List<String> vars = new ArrayList<>(UNIVERSE.length);
		Collections.addAll(vars, UNIVERSE);
		// shuffle-like selection
		List<String> selected = new ArrayList<>(UNIVERSE.length);
		for (String v : vars) {
			if (rnd.nextBoolean()) {
				selected.add(v);
			}
		}
		// ensure at least one var sometimes
		if (selected.isEmpty() && rnd.nextBoolean()) {
			selected.add(UNIVERSE[rnd.nextInt(UNIVERSE.length)]);
		}

		QueryBindingSet q = new QueryBindingSet();
		for (String name : selected) {
			// sometimes unbound (skip)
			if (rnd.nextInt(5) == 0) {
				continue;
			}
			q.setBinding(name, randomValueOrNull(rnd, salt + name.hashCode()));
		}
		// give some rows an explicit id for easier debugging
		if (q.getValue("id") == null && rnd.nextInt(3) == 0) {
			q.setBinding("id", VF.createIRI("urn:id:" + salt));
		}
		return q;
	}

	private static Value randomValueOrNull(Random rnd, int salt) {
		switch (rnd.nextInt(7)) {
		case 0:
			return null; // unbound-like
		case 1:
			return VF.createIRI("urn:res:" + (salt % 13));
		case 2:
			return VF.createLiteral(rnd.nextInt(5));
		case 3:
			return VF.createLiteral("s" + rnd.nextInt(5));
		case 4:
			return VF.createBNode("b" + Math.abs(salt % 5));
		case 5: {
			// typed literal distinct from string
			IRI dt = VF.createIRI("urn:dt:" + (salt % 3));
			return VF.createLiteral("v" + (salt % 7), dt);
		}
		default: {
			// language-tagged literal
			String lang = (salt % 2 == 0) ? "en" : "NO";
			return VF.createLiteral("l" + (salt % 7), lang);
		}
		}
	}

	private static List<BindingSet> referenceMinus(List<BindingSet> left, List<BindingSet> right) {
		List<BindingSet> out = new ArrayList<>(left.size());
		for (BindingSet L : left) {
			boolean eliminate = false;
			for (BindingSet R : right) {
				if (hasSharedBoundVar(L, R) && QueryResults.bindingSetsCompatible(L, R)) {
					eliminate = true;
					break;
				}
			}
			if (!eliminate) {
				out.add(L);
			}
		}
		return out;
	}

	private static boolean hasSharedBoundVar(BindingSet a, BindingSet b) {
		// Iterate the smaller set of bindings for efficiency
		int sizeA = sizeOfBound(a);
		int sizeB = sizeOfBound(b);
		BindingSet first = sizeA <= sizeB ? a : b;
		BindingSet second = sizeA <= sizeB ? b : a;
		for (Binding binding : first) {
			if (binding.getValue() == null)
				continue;
			if (second.getBinding(binding.getName()) != null && second.getValue(binding.getName()) != null) {
				return true;
			}
		}
		return false;
	}

	private static int sizeOfBound(BindingSet bs) {
		int n = 0;
		for (Binding ignored : bs) {
			n++;
		}
		return n;
	}

	private static List<String> replay(SPARQLMinusIteration it) {
		List<String> res = new ArrayList<>();
		try {
			while (it.hasNext()) {
				res.add(toKey(it.next()));
			}
		} finally {
			it.close();
		}
		return res;
	}

	private static Set<String> collect(List<String> rows) {
		return new HashSet<>(rows);
	}

	private static List<String> mapToKeys(List<BindingSet> rows) {
		List<String> out = new ArrayList<>(rows.size());
		for (BindingSet bs : rows) {
			out.add(toKey(bs));
		}
		return out;
	}

	private static String toKey(BindingSet bs) {
		// Build a canonical representation: sorted by variable name, include value type info
		List<String> parts = new ArrayList<>();
		for (Binding b : bs) {
			Value v = b.getValue();
			String vs;
			if (v instanceof Literal) {
				Literal lit = (Literal) v;
				String dt = lit.getDatatype() != null ? lit.getDatatype().stringValue() : "<no-dt>";
				String lang = lit.getLanguage().orElse("");
				vs = "L(" + lit.getLabel() + ")^" + dt + "@" + lang;
			} else if (v instanceof IRI) {
				vs = "I(" + v.stringValue() + ")";
			} else if (v instanceof BNode) {
				vs = "B(" + v.stringValue() + ")";
			} else {
				vs = v.toString();
			}
			parts.add(b.getName() + "=" + vs);
		}
		Collections.sort(parts);
		return String.join("|", parts);
	}
}
