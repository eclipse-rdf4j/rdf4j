/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class HashJoinTest {

	@Test
	public void testSimple() throws Exception {

		List<BindingSet> leftBlock = new ArrayList<>();
		leftBlock.add(bindingSet(binding("x", irid("p1"))));
		leftBlock.add(bindingSet(binding("x", irid("p2"))));
		leftBlock.add(bindingSet(binding("x", irid("p3"))));

		List<BindingSet> rightBlock = new ArrayList<>();
		rightBlock.add(bindingSet(binding("x", irid("p2"))));
		rightBlock.add(bindingSet(binding("x", irid("p1"))));
		rightBlock.add(bindingSet(binding("x", irid("p4"))));

		CloseableIteration<BindingSet, QueryEvaluationException> joinResultIter = HashJoin.join(leftBlock, rightBlock,
				Sets.newHashSet("x"),
				Collections.emptyList());
		List<BindingSet> joinResult = Iterations.asList(joinResultIter);

		Assertions.assertEquals(Lists.newArrayList(
				bindingSet(binding("x", irid("p1"))),
				bindingSet(binding("x", irid("p2")))),
				joinResult);
	}

	@Test
	public void testMultipleBindings() throws Exception {

		List<BindingSet> leftBlock = new ArrayList<>();
		leftBlock.add(bindingSet(binding("x", irid("p1")), binding("y", l("P1"))));
		leftBlock.add(bindingSet(binding("x", irid("p2")), binding("y", l("P2"))));

		List<BindingSet> rightBlock = new ArrayList<>();
		rightBlock.add(bindingSet(binding("x", irid("p2")), binding("z", l("something"))));

		CloseableIteration<BindingSet, QueryEvaluationException> joinResultIter = HashJoin.join(leftBlock, rightBlock,
				Sets.newHashSet("x"),
				Collections.emptyList());
		List<BindingSet> joinResult = Iterations.asList(joinResultIter);

		Assertions.assertEquals(1, joinResult.size());
		Assertions.assertEquals(
				bindingSet(binding("x", irid("p2")), binding("y", l("P2")), binding("z", l("something"))),
				joinResult.get(0));
	}

	@Test
	public void testLeftJoin_NoResultForBinding() throws Exception {

		List<BindingSet> leftBlock = new ArrayList<>();
		leftBlock.add(bindingSet(binding("x", irid("p1")), binding("y", l("P1"))));
		leftBlock.add(bindingSet(binding("y", l("P2"))));

		List<BindingSet> rightBlock = new ArrayList<>();
		rightBlock.add(bindingSet(binding("x", irid("p1")), binding("z", l("something"))));

		CloseableIteration<BindingSet, QueryEvaluationException> joinResultIter = HashJoin.join(leftBlock, rightBlock,
				Sets.newHashSet("x"),
				Collections.emptyList());
		List<BindingSet> joinResult = Iterations.asList(joinResultIter);

		Assertions.assertEquals(1, joinResult.size());
		Assertions.assertEquals(
				bindingSet(binding("x", irid("p1")), binding("y", l("P1")), binding("z", l("something"))),
				joinResult.get(0));
	}

	protected BindingSet bindingSet(Binding... bindings) {
		MapBindingSet bs = new MapBindingSet();
		for (Binding b : bindings) {
			bs.addBinding(b);
		}
		return bs;
	}

	protected Binding binding(String name, Value value) {
		return new SimpleBinding(name, value);
	}

	protected IRI irid(String localName) {
		return SimpleValueFactory.getInstance().createIRI(defaultNamespace(), localName);
	}

	protected Literal l(String literal) {
		return SimpleValueFactory.getInstance().createLiteral(literal);
	}

	protected String defaultNamespace() {
		return "http://example.org/";
	}
}
