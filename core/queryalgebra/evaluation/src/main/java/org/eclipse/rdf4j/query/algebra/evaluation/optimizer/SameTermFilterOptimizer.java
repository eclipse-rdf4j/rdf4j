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
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * A query optimizer that embeds {@link Filter}s with {@link SameTerm} operators in statement patterns as much as
 * possible. Operators like sameTerm(X, Y) are processed by renaming X to Y (or vice versa). Operators like sameTerm(X,
 * <someURI>) are processed by assigning the URI to all occurring variables with name X.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class SameTermFilterOptimizer implements QueryOptimizer {

	/**
	 * Applies generally applicable optimizations to the supplied query: variable assignments are inlined.
	 */
	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new SameTermFilterVisitor());
	}

	private static class SameTermFilterVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		protected SameTermFilterVisitor() {
			super(false);
		}

		@Override
		public void meet(Filter filter) {
			super.meet(filter);

			if (filter.getCondition() instanceof SameTerm) {
				// SameTerm applies to the filter's argument
				SameTerm sameTerm = (SameTerm) filter.getCondition();
				TupleExpr filterArg = filter.getArg();

				ValueExpr leftArg = sameTerm.getLeftArg();
				ValueExpr rightArg = sameTerm.getRightArg();

				// Verify that vars are (potentially) bound by filterArg
				Set<String> bindingNames = filterArg.getBindingNames();
				if (isUnboundVar(leftArg, bindingNames) || isUnboundVar(rightArg, bindingNames)) {
					// One or both var(s) are unbound, this expression will never
					// return any results
					filter.replaceWith(new EmptySet());
					return;
				}

				Set<String> assuredBindingNames = filterArg.getAssuredBindingNames();
				if (isUnboundVar(leftArg, assuredBindingNames) || isUnboundVar(rightArg, assuredBindingNames)) {
					// One or both var(s) are potentially unbound, inlining could
					// invalidate the result e.g. in case of left joins
					return;
				}

				if (leftArg instanceof Var || rightArg instanceof Var) {
					if (filterArg instanceof ArbitraryLengthPath && leftArg instanceof Var && rightArg instanceof Var) {
						final ArbitraryLengthPath alp = (ArbitraryLengthPath) filterArg;
						final List<Var> sameTermArgs = Arrays.asList((Var) leftArg, (Var) rightArg);

						if (sameTermArgs.contains(alp.getSubjectVar()) && sameTermArgs.contains(alp.getObjectVar())) {
							// SameTerm provides a deferred mapping to allow arbitrary-length property path to produce
							// cyclic paths. See SES-1685.
							// we can not inline.
							return;
						}
					}

					BindingSetAssignmentCollector collector = new BindingSetAssignmentCollector();
					filterArg.visit(collector);

					for (BindingSetAssignment bsa : collector.getBindingSetAssignments()) {
						// check if the VALUES clause / bindingsetassignment contains
						// one of the arguments of the sameTerm.
						// if so, we can not inline.
						Set<String> names = bsa.getAssuredBindingNames();
						if (leftArg instanceof Var) {
							if (names.contains(((Var) leftArg).getName())) {
								return;
							}
						}
						if (rightArg instanceof Var) {
							if (names.contains(((Var) rightArg).getName())) {
								return;
							}
						}
					}
				}

				Value leftValue = getValue(leftArg);
				Value rightValue = getValue(rightArg);

				if (leftValue != null && rightValue != null) {
					// ConstantOptimizer should have taken care of this
				} else if (leftValue != null && rightArg instanceof Var) {
					bindVar((Var) rightArg, leftValue, filter);
				} else if (rightValue != null && leftArg instanceof Var) {
					bindVar((Var) leftArg, rightValue, filter);
				} else if (leftArg instanceof Var && rightArg instanceof Var) {
					// Two unbound variables, rename rightArg to leftArg
					renameVar((Var) rightArg, (Var) leftArg, filter);
				}
			}
		}

		private boolean isUnboundVar(ValueExpr valueExpr, Set<String> bindingNames) {
			if (valueExpr instanceof Var) {
				Var var = (Var) valueExpr;
				return !var.hasValue() && !bindingNames.contains(var.getName());
			}
			return false;
		}

		private Value getValue(ValueExpr valueExpr) {
			if (valueExpr instanceof ValueConstant) {
				return ((ValueConstant) valueExpr).getValue();
			} else if (valueExpr instanceof Var) {
				return ((Var) valueExpr).getValue();
			} else {
				return null;
			}
		}

		private void renameVar(Var oldVar, Var newVar, Filter filter) {
			filter.getArg().visit(new VarRenamer(oldVar, newVar));

			// TODO: skip this step if old variable name is not used
			// Replace SameTerm-filter with an Extension, the old variable name
			// might still be relevant to nodes higher in the tree
			Extension extension = new Extension(filter.getArg());
			extension.addElement(new ExtensionElem(new Var(newVar.getName()), oldVar.getName()));
			filter.replaceWith(extension);
		}

		private void bindVar(Var var, Value value, Filter filter) {
			// Set the value on all occurences of the variable
			filter.getArg().visit(new VarBinder(var.getName(), value));
		}
	}

	private static class VarRenamer extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private final Var oldVar;

		private final Var newVar;

		public VarRenamer(Var oldVar, Var newVar) {
			super(true);
			this.oldVar = oldVar;
			this.newVar = newVar;
		}

		@Override
		public void meet(Var var) {
			if (var.equals(oldVar)) {
				var.replaceWith(newVar.clone());
			}
		}

		@Override
		public void meet(ProjectionElem projElem) throws RuntimeException {
			if (projElem.getName().equals(oldVar.getName())) {
				projElem.setName(newVar.getName());
			}
		}
	}

	private static class BindingSetAssignmentCollector extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private final List<BindingSetAssignment> assignments = new ArrayList<>();

		protected BindingSetAssignmentCollector() {
			super(true);
		}

		@Override
		public void meet(BindingSetAssignment bsa) {
			assignments.add(bsa);
		}

		public List<BindingSetAssignment> getBindingSetAssignments() {
			return assignments;
		}
	}

	private static class VarBinder extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private final String varName;

		private final Value value;

		public VarBinder(String varName, Value value) {
			super(true);
			this.varName = varName;
			this.value = value;
		}

		@Override
		public void meet(Var var) {
			if (var.getName().equals(varName)) {
				var.setValue(value);
			}
		}
	}
}
