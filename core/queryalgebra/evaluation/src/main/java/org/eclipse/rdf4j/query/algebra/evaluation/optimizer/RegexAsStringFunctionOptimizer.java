/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * A query optimizer that replaces REGEX with {@link FunctionCall}s that are equivalent operators
 *
 * @author Jerven Bolleman
 */
public class RegexAsStringFunctionOptimizer implements QueryOptimizer {

	private final ValueFactory vf;

	public RegexAsStringFunctionOptimizer(ValueFactory vf) {
		this.vf = vf;
	}

	/**
	 * Applies generally applicable optimizations to the supplied query: variable assignments are inlined.
	 */
	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new RegexAsStringFunctionVisitor(vf));
	}

	private static class RegexAsStringFunctionVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		private final ValueFactory vf;

		@Deprecated(forRemoval = true, since = "4.1.0")
		protected RegexAsStringFunctionVisitor() {
			vf = SimpleValueFactory.getInstance();
		}

		protected RegexAsStringFunctionVisitor(ValueFactory vf) {
			super(false);
			this.vf = vf;
		}

		@Override
		public void meet(Regex node) {
			final ValueExpr flagsArg = node.getFlagsArg();
			if (flagsArg == null || flagsArg.toString().isEmpty()) {
				// if we have no flags then we can not be in case insensitive mode
				if (node.getPatternArg() instanceof ValueConstant) {
					ValueConstant vc = (ValueConstant) node.getPatternArg();
					String regex = vc.getValue().stringValue();
					final boolean anchoredAtStart = regex.startsWith("^");
					final boolean anchoredAtEnd = regex.endsWith("$");

					if (anchoredAtStart && anchoredAtEnd) {
						equalsCandidate(node, regex);
					} else if (anchoredAtStart) {
						strstartsCandidate(node, regex);
					} else if (anchoredAtEnd) {
						strendsCandidate(node, regex);
					} else {
						containsCandidate(node, regex);
					}
				}
			}
			super.meet(node);
		}

		private void containsCandidate(Regex node, String potential) {
			if (plain(potential)) {
				node.replaceWith(new FunctionCall(FN.CONTAINS.stringValue(), node.getArg(), node.getPatternArg()));
			}
		}

		// If we have one of these chars it is likely to be an real regex.
		// If we are willing to peek in a Pattern object we could be sure
		// this seems a valid start with no regexes marked as simple strings
		private boolean plain(String potential) {
			for (int not : new char[] { '?', '*', '+', '{', '|', '\\', '.', '[', ']', '&', '(', ')' }) {
				if (potential.indexOf(not) != -1) {
					return false;
				}
			}
			return true;
		}

		private void strendsCandidate(Regex node, String regex) {
			final String potential = regex.substring(0, regex.length() - 1);
			if (plain(potential)) {
				ValueConstant vc = new ValueConstant(vf.createLiteral(potential));
				node.replaceWith(new FunctionCall(FN.ENDS_WITH.stringValue(), node.getArg(), vc));
			}
		}

		private void strstartsCandidate(Regex node, String regex) {
			final String potential = regex.substring(1, regex.length());
			if (plain(potential)) {
				ValueConstant vc = new ValueConstant(vf.createLiteral(potential));
				node.replaceWith(new FunctionCall(FN.STARTS_WITH.stringValue(), node.getArg(), vc));
			}
		}

		private void equalsCandidate(Regex node, String regex) {
			final String potential = regex.substring(1, regex.length() - 1);
			if (plain(potential)) {
				ValueConstant vc = new ValueConstant(vf.createLiteral(potential));
				node.replaceWith(new Compare(node.getArg(), vc, Compare.CompareOp.EQ));
			}
		}
	}
}
