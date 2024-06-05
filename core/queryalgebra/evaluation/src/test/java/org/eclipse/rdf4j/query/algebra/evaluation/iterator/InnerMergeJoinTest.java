/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ArrayBindingBasedQueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InnerMergeJoinTest {

	private final ArrayBindingBasedQueryEvaluationContext context = new ArrayBindingBasedQueryEvaluationContext(
			new QueryEvaluationContext.Minimal(null), new String[] { "a", "b", "c" }, null);
	private final Function<BindingSet, Value> valueFunction = context.getValue("a");
	private final Comparator<Value> cmp = Comparator.comparing(Value::stringValue);

	@Test
	public void testInnerMergeJoinEmpty() {
		try (InnerMergeJoinIterator innerMergeJoinIterator = new InnerMergeJoinIterator(
				new PeekMarkIterator<>(new EmptyIteration<>()), new PeekMarkIterator<>(new EmptyIteration<>()), null,
				null, null)) {
			boolean b = innerMergeJoinIterator.hasNext();
			assertFalse(b);
		}
	}

	@Test
	public void testInnerMergeJoinRemove() {
		try (var innerMergeJoinIterator = new InnerMergeJoinIterator(new PeekMarkIterator<>(new EmptyIteration<>()),
				new PeekMarkIterator<>(new EmptyIteration<>()), null, null, null)) {
			Assertions.assertThrows(UnsupportedOperationException.class, innerMergeJoinIterator::remove);
		}
	}

	@Test
	public void testInnerMergeJoinClose1() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"));
		var right = iterator(right("a1", "b_1"), right("a2", "b_2"));

		try (InnerMergeJoinIterator innerMergeJoinIterator = new InnerMergeJoinIterator(new PeekMarkIterator<>(left),
				new PeekMarkIterator<>(right), cmp, valueFunction, context)) {
			ArrayList<BindingSet> bindingSets = new ArrayList<>();
			while (innerMergeJoinIterator.hasNext()) {
				bindingSets.add(innerMergeJoinIterator.next());
				innerMergeJoinIterator.close();
			}

		}

	}

	@Test
	public void testInnerMergeJoinClose2() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"));
		var right = iterator(right("a1", "b_1"), right("a2", "b_2"));

		try (InnerMergeJoinIterator innerMergeJoinIterator = new InnerMergeJoinIterator(new PeekMarkIterator<>(left),
				new PeekMarkIterator<>(right), cmp, valueFunction, context)) {
			innerMergeJoinIterator.next();
			innerMergeJoinIterator.close();
			Assertions.assertThrows(NoSuchElementException.class, innerMergeJoinIterator::next);

		}

	}

	@Test
	public void testInnerMergeJoinNextNull() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"));
		var right = iterator(right("a1", "b_1"), right("a2", "b_2"));

		try (InnerMergeJoinIterator innerMergeJoinIterator = new InnerMergeJoinIterator(new PeekMarkIterator<>(left),
				new PeekMarkIterator<>(right), cmp, valueFunction, context)) {
			innerMergeJoinIterator.next();
			innerMergeJoinIterator.next();
			Assertions.assertThrows(NoSuchElementException.class, innerMergeJoinIterator::next);
		}

	}

	@Test
	public void testInnerMergeJoinRightNotResettable() {

		var left = iterator(left("a1", "b1"), left("a2", "b21"), left("a2", "b22"), left("a3", "b3"));
		var right = iterator(right("a1", "b_1"), right("a3", "b_3"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList("[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a3\";b=\"b3\";c=\"b_3\"]]"), result);

	}

	@Test
	public void testInnerMergeJoin1() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"));
		var right = iterator(right("a1", "b_1"), right("a2", "b_2"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList("[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a2\";b=\"b2\";c=\"b_2\"]]"), result);
	}

	@Test
	public void testInnerMergeJoinLeftRepeat() {

		var left = iterator(left("a1", "b1"), left("a1", "b2"), left("a2", "b3"));
		var right = iterator(right("a1", "b_1"), right("a2", "b_2"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(
				toList("[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b2\";c=\"b_1\"], [a=\"a2\";b=\"b3\";c=\"b_2\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinLeftRightRepeat() {

		var left = iterator(left("a1", "b1"), left("a1", "b2"), left("a2", "b3"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), right("a2", "b_3"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a1\";b=\"b2\";c=\"b_1\"], [a=\"a1\";b=\"b2\";c=\"b_2\"], [a=\"a2\";b=\"b3\";c=\"b_3\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinLeftRightRepeatNotMutable() {

		var left = iterator(left("a1", "b1"), left("a1", "b2"), left("a2", "b3"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), rightImmutable("a2", "b_3"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a1\";b=\"b2\";c=\"b_1\"], [a=\"a1\";b=\"b2\";c=\"b_2\"], [a=\"a2\";b=\"b3\";c=\"b_3\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinLeftRightRepeatMutableNotArrayBindingSet() {

		var left = iterator(left("a1", "b1"), left("a1", "b2"), left("a2", "b3"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), rightMutable("a2", "b_3"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a1\";b=\"b2\";c=\"b_1\"], [a=\"a1\";b=\"b2\";c=\"b_2\"], [a=\"a2\";b=\"b3\";c=\"b_3\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinLeftHolesRepeat() {

		var left = iterator(left("a1", "b1"), left("a1", "b2"), left("a2", "b3"), left("a3", "b4"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), right("a3", "b_4"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a1\";b=\"b2\";c=\"b_1\"], [a=\"a1\";b=\"b2\";c=\"b_2\"], [a=\"a3\";b=\"b4\";c=\"b_4\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinRightRepeat() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"), left("a3", "b3"), left("a4", "b4"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), right("a3", "b_3"), right("a3", "b_32"),
				right("a4", "b_4"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a3\";b=\"b3\";c=\"b_3\"], [a=\"a3\";b=\"b3\";c=\"b_32\"], [a=\"a4\";b=\"b4\";c=\"b_4\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinRightRepeatLeftEmptyFirst() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"), left("a3", "b3"), left("a4", "b4"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), right("a3", "b_3"), right("a3", "b_32"),
				right("a4", "b_4"), right("a5", "b_5"), right("a6", "b_6"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a3\";b=\"b3\";c=\"b_3\"], [a=\"a3\";b=\"b3\";c=\"b_32\"], [a=\"a4\";b=\"b4\";c=\"b_4\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinRightRepeatRightEmptyFirst() {

		var left = iterator(left("a1", "b1"), left("a2", "b2"), left("a3", "b3"), left("a4", "b4"), left("a5", "b5"),
				left("a5", "b52"), left("a6", "b6"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_2"), right("a3", "b_3"), right("a3", "b_32"),
				right("a4", "b_4"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_2\"], [a=\"a3\";b=\"b3\";c=\"b_3\"], [a=\"a3\";b=\"b3\";c=\"b_32\"], [a=\"a4\";b=\"b4\";c=\"b_4\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinRightBehind() {

		var left = iterator(left("a1", "b1"), left("a3", "b3"), left("a3", "b32"), left("a4", "b4"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_12"), right("a2", "b_2"), right("a22", "b_22"),
				right("a3", "b_3"), right("a4", "b_4"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_12\"], [a=\"a3\";b=\"b3\";c=\"b_3\"], [a=\"a3\";b=\"b32\";c=\"b_3\"], [a=\"a4\";b=\"b4\";c=\"b_4\"]]"),
				result);
	}

	@Test
	public void testInnerMergeJoinRightResettable() {

		var left = iterator(left("a1", "b1"), left("a3", "b31"), left("a3", "b32"), left("a3", "b33"),
				left("a4", "b4"));
		var right = iterator(right("a1", "b_1"), right("a1", "b_12"), right("a2", "b_2"), right("a22", "b_22"),
				right("a3", "b_31"), right("a3", "b_32"), right("a3", "b_33"));

		List<String> result = toString(innerMergeJoin(left, right));

		assertEquals(toList(
				"[[a=\"a1\";b=\"b1\";c=\"b_1\"], [a=\"a1\";b=\"b1\";c=\"b_12\"], [a=\"a3\";b=\"b31\";c=\"b_31\"], [a=\"a3\";b=\"b31\";c=\"b_32\"], [a=\"a3\";b=\"b31\";c=\"b_33\"], [a=\"a3\";b=\"b32\";c=\"b_31\"], [a=\"a3\";b=\"b32\";c=\"b_32\"], [a=\"a3\";b=\"b32\";c=\"b_33\"], [a=\"a3\";b=\"b33\";c=\"b_31\"], [a=\"a3\";b=\"b33\";c=\"b_32\"], [a=\"a3\";b=\"b33\";c=\"b_33\"]]"),
				result);
	}

	private List<String> toList(String input) {
		input = input.replace("[[", "[").replace("]]", "]");
		return Arrays.asList(input.split(", "));
	}

	private List<String> toString(List<BindingSet> result) {
		return result.stream().map(BindingSet::toString).collect(Collectors.toList());
	}

	private List<BindingSet> innerMergeJoin(CloseableIteration<BindingSet> left, CloseableIteration<BindingSet> right) {

		try (InnerMergeJoinIterator innerMergeJoinIterator = new InnerMergeJoinIterator(new PeekMarkIterator<>(left),
				new PeekMarkIterator<>(right), cmp, valueFunction, context)) {
			ArrayList<BindingSet> bindingSets = new ArrayList<>();
			while (innerMergeJoinIterator.hasNext()) {
				bindingSets.add(innerMergeJoinIterator.next());
			}

			return bindingSets;
		}
	}

	private BindingSet left(String v1, String v2) {
		ArrayBindingSet arrayBindingSet = context.createBindingSet();
		arrayBindingSet.addBinding("a", getLiteral(v1));
		arrayBindingSet.addBinding("b", getLiteral(v2));
		return arrayBindingSet;
	}

	Literal a2 = SimpleValueFactory.getInstance().createLiteral("a2");

	private Literal getLiteral(String v1) {
		if (v1 == "a2") {
			return a2;
		}

		return SimpleValueFactory.getInstance().createLiteral(v1);
	}

	private BindingSet right(String v1, String v2) {
		ArrayBindingSet arrayBindingSet = context.createBindingSet();
		arrayBindingSet.addBinding("a", getLiteral(v1));
		arrayBindingSet.addBinding("c", getLiteral(v2));
		return arrayBindingSet;
	}

	private BindingSet rightMutable(String v1, String v2) {
		ArrayBindingSet arrayBindingSet = context.createBindingSet();
		arrayBindingSet.addBinding("a", getLiteral(v1));
		arrayBindingSet.addBinding("c", getLiteral(v2));
		return new QueryBindingSet(arrayBindingSet);
	}

	private BindingSet rightImmutable(String v1, String v2) {
		return new ListBindingSet(List.of("a", "c"), getLiteral(v1), getLiteral(v2));
	}

	private CloseableIteration<BindingSet> iterator(BindingSet... bindingSets) {

		List<BindingSet> bindingSets1 = new ArrayList<>(List.of(bindingSets));
		bindingSets1.sort(Comparator.comparing(bindingSet -> bindingSet.getValue("a").stringValue()));

		return new CloseableIteratorIteration<>(bindingSets1.iterator());

	}

}
