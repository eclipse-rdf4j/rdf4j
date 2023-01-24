/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

class UnboundTest {

	private static final Predicate<BindingSet> IS_NOT_EMPTY = Predicate.not(BindingSet::isEmpty);

	public static Predicate<BindingSet> s(QueryEvaluationContext context, Var s) {
		return IS_NOT_EMPTY.and(hasTest(context, s.getName()));
	}

	public static Predicate<BindingSet> p(QueryEvaluationContext context, Var p) {
		return IS_NOT_EMPTY.and(hasTest(context, p.getName()));
	}

	public static Predicate<BindingSet> o(QueryEvaluationContext context, Var o) {
		return IS_NOT_EMPTY.and(hasTest(context, o.getName()));
	}

	public static Predicate<BindingSet> c(QueryEvaluationContext context, Var c) {
		return IS_NOT_EMPTY.and(hasTest(context, c.getName()));
	}

	public static Predicate<BindingSet> sp(QueryEvaluationContext context, Var s, Var p) {
		return IS_NOT_EMPTY.and(hasTest(context, s.getName())).and(hasTest(context, p.getName()));
	}

	public static Predicate<BindingSet> so(QueryEvaluationContext context, Var s, Var o) {
		return IS_NOT_EMPTY.and(hasTest(context, s.getName())).and(hasTest(context, o.getName()));
	}

	public static Predicate<BindingSet> sc(QueryEvaluationContext context, Var s, Var c) {
		Predicate<BindingSet> sTest = hasTest(context, s.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(sTest.or(cTest));
	}

	public static Predicate<BindingSet> po(QueryEvaluationContext context, Var p, Var o) {
		Predicate<BindingSet> pTest = hasTest(context, p.getName());
		Predicate<BindingSet> oTest = hasTest(context, o.getName());

		return IS_NOT_EMPTY.and(pTest.or(oTest));
	}

	public static Predicate<BindingSet> pc(QueryEvaluationContext context, Var p, Var c) {
		Predicate<BindingSet> pTest = hasTest(context, p.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(pTest.or(cTest));
	}

	public static Predicate<BindingSet> oc(QueryEvaluationContext context, Var o, Var c) {
		Predicate<BindingSet> oTest = hasTest(context, o.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(oTest.or(cTest));
	}

	public static Predicate<BindingSet> spo(QueryEvaluationContext context, Var s, Var p, Var o) {
		Predicate<BindingSet> sTest = hasTest(context, s.getName());
		Predicate<BindingSet> pTest = hasTest(context, p.getName());
		Predicate<BindingSet> oTest = hasTest(context, o.getName());

		return IS_NOT_EMPTY.and(sTest.or(pTest).or(oTest));
	}

	public static Predicate<BindingSet> spc(QueryEvaluationContext context, Var s, Var p, Var c) {

		Predicate<BindingSet> sTest = hasTest(context, s.getName());
		Predicate<BindingSet> pTest = hasTest(context, p.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(sTest.or(pTest).or(cTest));
	}

	public static Predicate<BindingSet> soc(QueryEvaluationContext context, Var s, Var o, Var c) {

		Predicate<BindingSet> sTest = hasTest(context, s.getName());
		Predicate<BindingSet> oTest = hasTest(context, o.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(sTest.or(oTest).or(cTest));
	}

	public static Predicate<BindingSet> poc(QueryEvaluationContext context, Var p, Var o, Var c) {
		Predicate<BindingSet> pTest = hasTest(context, p.getName());
		Predicate<BindingSet> oTest = hasTest(context, o.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(pTest.or(oTest).or(cTest));
	}

	public static Predicate<BindingSet> spoc(QueryEvaluationContext context, Var s, Var p, Var o,
			Var c) {

		Predicate<BindingSet> pTest = hasTest(context, p.getName());
		Predicate<BindingSet> sTest = hasTest(context, s.getName());
		Predicate<BindingSet> oTest = hasTest(context, o.getName());
		Predicate<BindingSet> cTest = hasTest(context, c.getName());

		return IS_NOT_EMPTY.and(sTest.or(pTest).or(oTest).or(cTest));
	}

	private static Predicate<BindingSet> hasTest(QueryEvaluationContext context, String name) {
		Predicate<BindingSet> oHasBinding = context.hasBinding(name);
		Function<BindingSet, Value> oGetValue = context.getValue(name);

		return bindings -> oHasBinding.test(bindings) && oGetValue.apply(bindings) == null;
	}
}
