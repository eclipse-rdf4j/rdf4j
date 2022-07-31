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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.Test;

/**
 * @author Bart Hanssens
 */
public class GroupIteratorTest {
	private final static ValueFactory vf = SimpleValueFactory.getInstance();
	private final EvaluationStrategy evaluator = new StrictEvaluationStrategy(null, null);
	private final QueryEvaluationContext context = new QueryEvaluationContext.Minimal(
			vf.createLiteral(Date.from(Instant.now())), null);

	private final static BindingSetAssignment EMPTY_ASSIGNMENT;

	static {
		EMPTY_ASSIGNMENT = new BindingSetAssignment();
		EMPTY_ASSIGNMENT.setBindingSets(Collections.emptyList());
	}

	private final static BindingSetAssignment NONEMPTY_ASSIGNMENT;

	static {
		NONEMPTY_ASSIGNMENT = new BindingSetAssignment();
		ArrayList<BindingSet> list = new ArrayList<>();
		for (int i = 1; i < 10; i++) {
			QueryBindingSet bindings = new QueryBindingSet();
			bindings.addBinding("a", vf.createLiteral(i));
			list.add(bindings);
		}
		NONEMPTY_ASSIGNMENT.setBindingSets(list);
	}

	@Test
	public void testAvgEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("avg", new Avg(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("avg").getValue())
					.describedAs("AVG on empty set should result in 0")
					.isEqualTo(vf.createLiteral("0", XSD.INTEGER));
		}
	}

	@Test
	public void testMaxEmptySet_DefaultGroup() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("max", new Max(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isTrue();
			assertThat(gi.next().size()).isEqualTo(0);
		}
	}

	@Test
	public void testMaxEmptySet_Grouped() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("max", new Max(new Var("a"))));
		group.addGroupBindingName("x"); // we are grouping by variable x

		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isFalse();
		}
	}

	@Test
	public void testMinEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("min", new Min(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isTrue();
			assertThat(gi.next().size()).isEqualTo(0);
		}
	}

	@Test
	public void testSampleEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("sample", new Sample(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isTrue();
			assertThat(gi.next().size()).isEqualTo(0);
		}
	}

	@Test
	public void testGroupConcatEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("groupconcat", new GroupConcat(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("groupconcat").getValue())
					.describedAs("GROUP_CONCAT on empty set should result in empty string")
					.isEqualTo(vf.createLiteral(""));
		}
	}

	@Test
	public void testAvgNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("avg", new Avg(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("avg").getValue()).isEqualTo(vf.createLiteral("5", XSD.DECIMAL));
		}
	}

	@Test
	public void testCountNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("count", new Count(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("count").getValue()).isEqualTo(vf.createLiteral("9", XSD.INTEGER));
		}
	}

	@Test
	public void testSumNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("sum", new Sum(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("sum").getValue()).isEqualTo(vf.createLiteral("45", XSD.INTEGER));
		}
	}
}
