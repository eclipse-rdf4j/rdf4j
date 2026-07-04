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

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

class StatementConvertor {
	public static BiConsumer<MutableBindingSet, Statement> s(QueryEvaluationContext context, Var s) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());
		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> p(QueryEvaluationContext context, Var p) {
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());
		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> o(QueryEvaluationContext context, Var o) {
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());
		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> c(QueryEvaluationContext context, Var c) {
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());
		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> sp(QueryEvaluationContext context, Var s, Var p) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}

		};

	}

	public static BiConsumer<MutableBindingSet, Statement> so(QueryEvaluationContext context, Var s, Var o) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();

			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> sc(QueryEvaluationContext context, Var s, Var c) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}

			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> po(QueryEvaluationContext context, Var p, Var o) {

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> pc(QueryEvaluationContext context, Var p, Var c) {

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}

			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> oc(QueryEvaluationContext context, Var o, Var c) {

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> spo(QueryEvaluationContext context, Var s, Var p, Var o) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}

		};

	}

	public static BiConsumer<MutableBindingSet, Statement> spc(QueryEvaluationContext context, Var s, Var p, Var c) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}
			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}

		};

	}

	public static BiConsumer<MutableBindingSet, Statement> soc(QueryEvaluationContext context, Var s, Var o, Var c) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();

			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> poc(QueryEvaluationContext context, Var p, Var o, Var c) {

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};

	}

	public static BiConsumer<MutableBindingSet, Statement> spoc(QueryEvaluationContext context, Var s, Var p, Var o,
			Var c) {

		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		Predicate<BindingSet> sIsSet = context.hasBinding(s.getName());

		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		Predicate<BindingSet> pIsSet = context.hasBinding(p.getName());

		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		Predicate<BindingSet> oIsSet = context.hasBinding(o.getName());

		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		Predicate<BindingSet> cIsSet = context.hasBinding(c.getName());

		return (result, st) -> {
			boolean empty = result.isEmpty();
			if (empty || !sIsSet.test(result)) {
				setS.accept(st.getSubject(), result);
			}
			if (empty || !pIsSet.test(result)) {
				setP.accept(st.getPredicate(), result);
			}
			if (empty || !oIsSet.test(result)) {
				setO.accept(st.getObject(), result);
			}
			if (empty || !cIsSet.test(result)) {
				setC.accept(st.getContext(), result);
			}
		};

	}

}
