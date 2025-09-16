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
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/**
 * Rewrite OPTIONAL { P . FILTER(?r = ?l) } where ?l is bound on the LHS and ?r is local to RHS into OPTIONAL { P[?r :=
 * ?l] . BIND(?l AS ?r) }.
 *
 * The rewrite is conservative: - we only rewrite equality conditions of the form SameTerm(?r, ?l) or (?r = ?l) - and
 * only when one var is provably on the left and the other on the right - and the "right" var occurs in
 * subject/predicate/context position of a StatementPattern (so it can’t be a plain literal-only binding).
 *
 * This mirrors Jena’s TransformImplicitLeftJoin pattern but in RDF4J algebra.
 */
public class ImplicitLeftJoinOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new Rewriter());
	}

	private static final class Rewriter extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meet(LeftJoin lj) {
			// rewrite children bottom-up first
			super.meet(lj);

			TupleExpr right = lj.getRightArg();
			if (!(right instanceof Filter)) {
				return;
			}
			Filter f = (Filter) right;

			// Extract candidate var=var equalities from the Filter condition
			List<VarEq> eqs = new ArrayList<>();
			collectVarEqs(f.getCondition(), eqs);

			if (eqs.isEmpty()) {
				return;
			}

			// Vars on each side
			Set<String> leftVars = VarNameCollector.process(lj.getLeftArg());
			Set<String> rightVars = VarNameCollector.process(f.getArg()); // RHS inner pattern (without the filter)

			// Try to find a pair (?r, ?l) such that r is only-right and l is (also) left
			for (VarEq eq : eqs) {
				EqRole role = classify(eq, leftVars, rightVars);
				if (!role.rewritable) {
					continue;
				}

				// Check "rightVar" occurs in a position that is not only object literal
				if (!rightVarOccursInNonLiteralPosition(f.getArg(), role.rightVar)) {
					continue;
				}

				// 1) remove this equality from the filter condition (compute residual)
				ValueExpr residual = removeEq(f.getCondition(), eq);

				// 2) rename all occurrences of "rightVar" to "leftVar" inside RHS pattern
				renameVarIn(f.getArg(), role.rightVar, role.leftVar);

				// 3) wrap RHS with BIND(?left as ?right) if names differ
				TupleExpr newRight = f.getArg();
				if (!role.rightVar.equals(role.leftVar)) {
					Extension ext = new Extension(newRight);
					ext.addElement(new ExtensionElem(Var.of(role.leftVar), role.rightVar));
					newRight = ext;
				}

				// 4) if residual filter still has content, keep it
				if (residual != null) {
					lj.setRightArg(new Filter(newRight, residual));
				} else {
					lj.setRightArg(newRight);
				}
				// Done for the first applicable equality
				break;
			}
		}

		/** Represents an equality between two (Var, Var). */
		private static final class VarEq {
			final String a, b;

			VarEq(String a, String b) {
				this.a = a;
				this.b = b;
			}

			boolean matches(String x, String y) {
				return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
			}
		}

		/** Which is the left-bound var and which is strictly-right var. */
		private static final class EqRole {
			final boolean rewritable;
			final String leftVar, rightVar;

			EqRole(boolean rewritable, String leftVar, String rightVar) {
				this.rewritable = rewritable;
				this.leftVar = leftVar;
				this.rightVar = rightVar;
			}

			static EqRole not() {
				return new EqRole(false, null, null);
			}
		}

		private static EqRole classify(VarEq eq, Set<String> leftVars, Set<String> rightVars) {
			boolean aL = leftVars.contains(eq.a), bL = leftVars.contains(eq.b);
			boolean aR = rightVars.contains(eq.a), bR = rightVars.contains(eq.b);
			// Must be exactly one from left and one from right (avoid accidental both-sides)
			if (aL && bR && !aR) {
				return new EqRole(true, eq.a, eq.b);
			}
			if (bL && aR && !bR) {
				return new EqRole(true, eq.b, eq.a);
			}
			return EqRole.not();
		}

		private static void collectVarEqs(ValueExpr e, List<VarEq> out) {
			if (e == null) {
				return;
			}
			if (e instanceof SameTerm) {
				SameTerm st = (SameTerm) e;
				if (st.getLeftArg() instanceof Var && st.getRightArg() instanceof Var) {
					out.add(new VarEq(((Var) st.getLeftArg()).getName(), ((Var) st.getRightArg()).getName()));
				}
				return;
			}
			if (e instanceof Compare) {
				Compare cmp = (Compare) e;
				if (cmp.getOperator() == Compare.CompareOp.EQ
						&& cmp.getLeftArg() instanceof Var && cmp.getRightArg() instanceof Var) {
					out.add(new VarEq(((Var) cmp.getLeftArg()).getName(), ((Var) cmp.getRightArg()).getName()));
				}
				return;
			}
			if (e instanceof And) {
				And a = (And) e;
				collectVarEqs(a.getLeftArg(), out);
				collectVarEqs(a.getRightArg(), out);
			}
			// others ignored (OR, NOT, etc.)
		}

		private static boolean rightVarOccursInNonLiteralPosition(TupleExpr expr, String var) {
			// ensure var appears as subj/pred/ctx of some StatementPattern (safe IRI/BNODE position)
			List<StatementPattern> sps = org.eclipse.rdf4j.query.algebra.helpers.collectors.StatementPatternCollector
					.process(expr);
			for (StatementPattern sp : sps) {
				if (isVar(sp.getSubjectVar(), var) || isVar(sp.getPredicateVar(), var)
						|| isVar(sp.getContextVar(), var)) {
					return true;
				}
			}
			return false;
		}

		private static boolean isVar(Var v, String name) {
			return v != null && !v.hasValue() && name.equals(v.getName());
		}

		/** Remove a specific var=var equality (where present) from a (possibly conjunctive) condition. */
		private static ValueExpr removeEq(ValueExpr cond, VarEq target) {
			if (cond == null) {
				return null;
			}
			if (isEq(cond, target)) {
				return null; // removed entirely
			}
			if (cond instanceof And) {
				And a = (And) cond;
				ValueExpr l = removeEq(a.getLeftArg(), target);
				ValueExpr r = removeEq(a.getRightArg(), target);
				if (l == null) {
					return r;
				}
				if (r == null) {
					return l;
				}
				if (l == a.getLeftArg() && r == a.getRightArg()) {
					return cond; // unchanged
				}
				return new And(l, r);
			}
			// other nodes: unchanged
			return cond;
		}

		private static boolean isEq(ValueExpr e, VarEq v) {
			if (e instanceof SameTerm) {
				SameTerm st = (SameTerm) e;
				if (st.getLeftArg() instanceof Var && st.getRightArg() instanceof Var) {
					return v.matches(((Var) st.getLeftArg()).getName(), ((Var) st.getRightArg()).getName());
				}
			} else if (e instanceof Compare) {
				Compare cmp = (Compare) e;
				if (cmp.getOperator() == Compare.CompareOp.EQ
						&& cmp.getLeftArg() instanceof Var && cmp.getRightArg() instanceof Var) {
					return v.matches(((Var) cmp.getLeftArg()).getName(), ((Var) cmp.getRightArg()).getName());
				}
			}
			return false;
		}

		/** In-place rename of a var name across a TupleExpr. */
		private static void renameVarIn(TupleExpr expr, String from, String to) {
			expr.visit(new AbstractQueryModelVisitor<RuntimeException>() {
				@Override
				public void meet(Var node) {
					if (!node.hasValue() && from.equals(node.getName())) {
						Var var = Var.of(to, node.getValue(), node.isAnonymous(), node.isConstant());
						node.replaceWith(var);
					}
				}
			});
		}
	}
}
