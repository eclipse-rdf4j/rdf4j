/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

class StatementConvertorWithoutBindingChecks {

	private StatementConvertorWithoutBindingChecks() {
	}

	public static BiConsumer<MutableBindingSet, Statement> s(QueryEvaluationContext context, Var s) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		return (result, st) -> setS.accept(st.getSubject(), result);
	}

	public static BiConsumer<MutableBindingSet, Statement> p(QueryEvaluationContext context, Var p) {
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		return (result, st) -> setP.accept(st.getPredicate(), result);
	}

	public static BiConsumer<MutableBindingSet, Statement> o(QueryEvaluationContext context, Var o) {
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		return (result, st) -> setO.accept(st.getObject(), result);
	}

	public static BiConsumer<MutableBindingSet, Statement> c(QueryEvaluationContext context, Var c) {
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> setC.accept(st.getContext(), result);
	}

	public static BiConsumer<MutableBindingSet, Statement> sp(QueryEvaluationContext context, Var s, Var p) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setP.accept(st.getPredicate(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> so(QueryEvaluationContext context, Var s, Var o) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setO.accept(st.getObject(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> sc(QueryEvaluationContext context, Var s, Var c) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setC.accept(st.getContext(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> po(QueryEvaluationContext context, Var p, Var o) {
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		return (result, st) -> {
			setP.accept(st.getPredicate(), result);
			setO.accept(st.getObject(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> pc(QueryEvaluationContext context, Var p, Var c) {
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setP.accept(st.getPredicate(), result);
			setC.accept(st.getContext(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> oc(QueryEvaluationContext context, Var o, Var c) {
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setO.accept(st.getObject(), result);
			setC.accept(st.getContext(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> spo(QueryEvaluationContext context, Var s, Var p, Var o) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setP.accept(st.getPredicate(), result);
			setO.accept(st.getObject(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> spc(QueryEvaluationContext context, Var s, Var p, Var c) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setP.accept(st.getPredicate(), result);
			setC.accept(st.getContext(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> soc(QueryEvaluationContext context, Var s, Var o, Var c) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setO.accept(st.getObject(), result);
			setC.accept(st.getContext(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> poc(QueryEvaluationContext context, Var p, Var o, Var c) {
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setP.accept(st.getPredicate(), result);
			setO.accept(st.getObject(), result);
			setC.accept(st.getContext(), result);
		};
	}

	public static BiConsumer<MutableBindingSet, Statement> spoc(QueryEvaluationContext context, Var s, Var p, Var o,
			Var c) {
		BiConsumer<Value, MutableBindingSet> setS = context.addBinding(s.getName());
		BiConsumer<Value, MutableBindingSet> setP = context.addBinding(p.getName());
		BiConsumer<Value, MutableBindingSet> setO = context.addBinding(o.getName());
		BiConsumer<Value, MutableBindingSet> setC = context.addBinding(c.getName());
		return (result, st) -> {
			setS.accept(st.getSubject(), result);
			setP.accept(st.getPredicate(), result);
			setO.accept(st.getObject(), result);
			setC.accept(st.getContext(), result);
		};
	}
}
