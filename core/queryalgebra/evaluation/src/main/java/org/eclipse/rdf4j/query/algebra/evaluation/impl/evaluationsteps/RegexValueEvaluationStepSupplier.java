/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep.ConstantQueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

public class RegexValueEvaluationStepSupplier {
	/**
	 * Returns value evaluation steps that determines whether the two operands match according to the <code>regex</code>
	 * operator.
	 *
	 * If possible it will cache the Pattern and flags, and if everything is constant it will return a constant value.
	 */
	private static final class ChangingRegexQueryValueEvaluationStep implements QueryValueEvaluationStep {
		private final Regex node;
		private final EvaluationStrategy strategy;

		private ChangingRegexQueryValueEvaluationStep(Regex node, EvaluationStrategy strategy) {
			this.node = node;
			this.strategy = strategy;
		}

		@Override
		public Value evaluate(BindingSet bindings) throws QueryEvaluationException {
			Value arg = strategy.evaluate(node.getArg(), bindings);
			Value parg = strategy.evaluate(node.getPatternArg(), bindings);
			Value farg = null;
			ValueExpr flagsArg = node.getFlagsArg();
			if (flagsArg != null) {
				farg = strategy.evaluate(flagsArg, bindings);
			}

			if (QueryEvaluationUtility.isStringLiteral(arg) && QueryEvaluationUtility.isSimpleLiteral(parg)
					&& (farg == null || QueryEvaluationUtility.isSimpleLiteral(farg))) {
				String text = ((Literal) arg).getLabel();
				String ptn = ((Literal) parg).getLabel();
				// TODO should this Pattern be cached?
				int f = extractRegexFlags(farg);
				Pattern pattern = Pattern.compile(ptn, f);
				boolean result = pattern.matcher(text).find();
				return BooleanLiteral.valueOf(result);
			}
			throw new ValueExprEvaluationException();
		}
	}

	public static QueryValueEvaluationStep make(EvaluationStrategy strategy, Regex node,
			QueryEvaluationContext context) {
		QueryValueEvaluationStep argStep = strategy.precompile(node.getArg(), context);
		QueryValueEvaluationStep pargStep = strategy.precompile(node.getPatternArg(), context);
		QueryValueEvaluationStep fargStep = null;

		ValueExpr flagsArg = node.getFlagsArg();
		if (flagsArg != null) {
			fargStep = strategy.precompile(flagsArg, context);
		}
		if (argStep.isConstant() && pargStep.isConstant() && (flagsArg == null || fargStep.isConstant())) {
			return allRegexPartsAreConstant(argStep, pargStep, fargStep, flagsArg);
		} else if (pargStep.isConstant() && (flagsArg == null || fargStep.isConstant())) {
			return regexAndFlagsAreConstant(argStep, pargStep, fargStep, flagsArg);
		} else {
			return new ChangingRegexQueryValueEvaluationStep(node, strategy);
		}
	}

	private static QueryValueEvaluationStep regexAndFlagsAreConstant(QueryValueEvaluationStep argStep,
			QueryValueEvaluationStep pargStep, QueryValueEvaluationStep fargStep, ValueExpr flagsArg) {
		Value parg = pargStep.evaluate(EmptyBindingSet.getInstance());
		Value farg = null;
		if (flagsArg != null) {
			farg = fargStep.evaluate(EmptyBindingSet.getInstance());
		}
		if (QueryEvaluationUtility.isSimpleLiteral(parg)
				&& (farg == null || QueryEvaluationUtility.isSimpleLiteral(farg))) {
			String ptn = ((Literal) parg).getLabel();
			int f = extractRegexFlags(farg);
			Pattern pattern = Pattern.compile(ptn, f);

			return bindings -> {
				Value arg = argStep.evaluate(bindings);
				if (QueryEvaluationUtility.isStringLiteral(arg)) {
					String text = ((Literal) arg).getLabel();
					boolean result = pattern.matcher(text).find();
					return BooleanLiteral.valueOf(result);
				}
				throw new ValueExprEvaluationException();
			};
		}
		throw new ValueExprEvaluationException();
	}

	private static QueryValueEvaluationStep allRegexPartsAreConstant(QueryValueEvaluationStep argStep,
			QueryValueEvaluationStep pargStep,
			QueryValueEvaluationStep fargStep, ValueExpr flagsArg) {
		Value arg = argStep.evaluate(EmptyBindingSet.getInstance());
		Value parg = pargStep.evaluate(EmptyBindingSet.getInstance());
		Value farg = null;
		if (flagsArg != null) {
			farg = fargStep.evaluate(EmptyBindingSet.getInstance());
		}
		if (QueryEvaluationUtility.isStringLiteral(arg) && QueryEvaluationUtility.isSimpleLiteral(parg)
				&& (farg == null || QueryEvaluationUtility.isSimpleLiteral(farg))) {
			String text = ((Literal) arg).getLabel();
			String ptn = ((Literal) parg).getLabel();
			int f = extractRegexFlags(farg);
			Pattern pattern = Pattern.compile(ptn, f);
			boolean result = pattern.matcher(text).find();
			BooleanLiteral valueOf = BooleanLiteral.valueOf(result);
			return new ConstantQueryValueEvaluationStep(valueOf);
		}
		throw new ValueExprEvaluationException();
	}

	private static int extractRegexFlags(Value farg) {
		String flags = "";
		if (farg != null) {
			flags = ((Literal) farg).getLabel();
		}
		int f = 0;
		for (char c : flags.toCharArray()) {
			switch (c) {
			case 's':
				f |= Pattern.DOTALL;
				break;
			case 'm':
				f |= Pattern.MULTILINE;
				break;
			case 'i':
				f |= Pattern.CASE_INSENSITIVE;
				f |= Pattern.UNICODE_CASE;
				break;
			case 'x':
				f |= Pattern.COMMENTS;
				break;
			case 'd':
				f |= Pattern.UNIX_LINES;
				break;
			case 'u':
				f |= Pattern.UNICODE_CASE;
				break;
			case 'q':
				f |= Pattern.LITERAL;
				break;
			default:
				throw new ValueExprEvaluationException(flags);
			}
		}
		return f;
	}
}
