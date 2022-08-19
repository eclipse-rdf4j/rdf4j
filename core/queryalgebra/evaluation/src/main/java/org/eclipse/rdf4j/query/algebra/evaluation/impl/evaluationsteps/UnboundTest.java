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
	public static Predicate<BindingSet> s(QueryEvaluationContext context, Var s) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null;
			}
			return false;
		};
	}

	public static Predicate<BindingSet> p(QueryEvaluationContext context, Var p) {
		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return pHasBinding.test(bindings) && pGetValue.apply(bindings) == null;
			}
			return false;
		};
	}

	public static Predicate<BindingSet> o(QueryEvaluationContext context, Var o) {
		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return oHasBinding.test(bindings) && oGetValue.apply(bindings) == null;
			}
			return false;
		};
	}

	public static Predicate<BindingSet> c(QueryEvaluationContext context, Var c) {
		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};
	}

	public static Predicate<BindingSet> sp(QueryEvaluationContext context, Var s, Var p) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null &&
						pHasBinding.test(bindings) && pGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> so(QueryEvaluationContext context, Var s, Var o) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null
						|| oHasBinding.test(bindings) && oGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> sc(QueryEvaluationContext context, Var s, Var c) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> po(QueryEvaluationContext context, Var p, Var o) {

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return pHasBinding.test(bindings) && pGetValue.apply(bindings) == null
						|| oHasBinding.test(bindings) && oGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> pc(QueryEvaluationContext context, Var p, Var c) {

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return pHasBinding.test(bindings) && pGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> oc(QueryEvaluationContext context, Var o, Var c) {

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return oHasBinding.test(bindings) && oGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> spo(QueryEvaluationContext context, Var s, Var p, Var o) {
		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null
						|| pHasBinding.test(bindings) && pGetValue.apply(bindings) == null
						|| oHasBinding.test(bindings) && oGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> spc(QueryEvaluationContext context, Var s, Var p, Var c) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null
						|| pHasBinding.test(bindings) && pGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> soc(QueryEvaluationContext context, Var s, Var o, Var c) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null
						|| oHasBinding.test(bindings) && oGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> poc(QueryEvaluationContext context, Var p, Var o, Var c) {

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return pHasBinding.test(bindings) && pGetValue.apply(bindings) == null
						|| oHasBinding.test(bindings) && oGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

	public static Predicate<BindingSet> spoc(QueryEvaluationContext context, Var s, Var p, Var o,
			Var c) {

		Predicate<BindingSet> sHasBinding = context.hasBinding(s.getName());
		Function<BindingSet, Value> sGetValue = context.getValue(s.getName());

		Predicate<BindingSet> pHasBinding = context.hasBinding(p.getName());
		Function<BindingSet, Value> pGetValue = context.getValue(p.getName());

		Predicate<BindingSet> oHasBinding = context.hasBinding(o.getName());
		Function<BindingSet, Value> oGetValue = context.getValue(o.getName());

		Predicate<BindingSet> cHasBinding = context.hasBinding(c.getName());
		Function<BindingSet, Value> cGetValue = context.getValue(c.getName());

		return bindings -> {
			if (!bindings.isEmpty()) {
				return sHasBinding.test(bindings) && sGetValue.apply(bindings) == null
						|| pHasBinding.test(bindings) && pGetValue.apply(bindings) == null
						|| oHasBinding.test(bindings) && oGetValue.apply(bindings) == null
						|| cHasBinding.test(bindings) && cGetValue.apply(bindings) == null;
			}
			return false;
		};

	}

}
