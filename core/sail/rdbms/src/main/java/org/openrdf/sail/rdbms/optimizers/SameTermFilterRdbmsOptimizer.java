/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.optimizers;

import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.SameTerm;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * A query optimizer that embeds {@link Filter}s with {@link SameTerm} operators
 * in statement patterns as much as possible. Operators like sameTerm(X, Y) are
 * processed by renaming X to Y (or vice versa). Operators like sameTerm(X,
 * <someURI>) are processed by assigning the URI to all occurring variables with
 * name X.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class SameTermFilterRdbmsOptimizer implements QueryOptimizer {

	/**
	 * Applies generally applicable optimizations to the supplied query: variable
	 * assignments are inlined.
	 */
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new SameTermFilterVisitor());
	}

	protected static class SameTermFilterVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meet(Filter filter) {
			super.meet(filter);

			if (filter.getCondition() instanceof SameTerm) {
				// SameTerm applies to the filter's argument
				SameTerm sameTerm = (SameTerm)filter.getCondition();
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
				
				Value leftValue = getValue(leftArg);
				Value rightValue = getValue(rightArg);
				
				if (leftValue != null && rightValue != null) {
					// ConstantOptimizer should have taken care of this
				}
				else if (leftValue != null && rightArg instanceof Var) {
					bindVar((Var)rightArg, leftValue, filter);
				}
				else if (rightValue != null && leftArg instanceof Var) {
					bindVar((Var)leftArg, rightValue, filter);
				}
				else if (leftArg instanceof Var && rightArg instanceof Var) {
					// Two unbound variables, rename rightArg to leftArg
					renameVar((Var)rightArg, (Var)leftArg, filter);
				}
			}
		}

		private boolean isUnboundVar(ValueExpr valueExpr, Set<String> bindingNames) {
			if (valueExpr instanceof Var) {
				Var var = (Var)valueExpr;
				return !var.hasValue() && !bindingNames.contains(var.getName());
			}
			return false;
		}

		private Value getValue(ValueExpr valueExpr) {
			if (valueExpr instanceof ValueConstant) {
				return ((ValueConstant)valueExpr).getValue();
			}
			else if (valueExpr instanceof Var) {
				return ((Var)valueExpr).getValue();
			}
			else {
				return null;
			}
		}

		private void renameVar(Var oldVar, Var newVar, Filter filter) {
			filter.getArg().visit(new VarRenamer(oldVar.getName(), newVar.getName()));

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

			// Get rid of the filter
			filter.replaceWith(filter.getArg());
		}
	}

	protected static class VarRenamer extends AbstractQueryModelVisitor<RuntimeException> {

		private final String oldName;

		private final String newName;

		public VarRenamer(String oldName, String newName) {
			this.oldName = oldName;
			this.newName = newName;
		}

		@Override
		public void meet(Var var) {
			if (var.getName().equals(oldName)) {
				var.setName(newName);
			}
		}

		@Override
		public void meet(ProjectionElem projElem)
			throws RuntimeException
		{
			if (projElem.getSourceName().equals(oldName)) {
				projElem.setSourceName(newName);
			}
		}
	}

	protected static class VarBinder extends AbstractQueryModelVisitor<RuntimeException> {

		private final String varName;

		private final Value value;

		public VarBinder(String varName, Value value) {
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
