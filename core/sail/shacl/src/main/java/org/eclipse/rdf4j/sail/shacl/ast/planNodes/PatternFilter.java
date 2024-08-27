/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class PatternFilter extends FilterPlanNode {

	private static final Logger logger = LoggerFactory.getLogger(PatternFilter.class);

	private final Pattern pattern;

	public PatternFilter(PlanNode parent, String pattern, String flags) {
		super(parent);
		if (flags != null && !flags.isEmpty()) {
			int flag = 0b0;

			if (flags.contains("i")) {
				flag = flag | Pattern.CASE_INSENSITIVE;
				logger.trace("PatternFilter constructed with case insensitive flag");
			}

			if (flags.contains("d")) {
				flag = flag | Pattern.UNIX_LINES;
				logger.trace("PatternFilter constructed with UNIX lines flag");
			}

			if (flags.contains("m")) {
				flag = flag | Pattern.MULTILINE;
				logger.trace("PatternFilter constructed with multiline flag");
			}

			if (flags.contains("s")) {
				flag = flag | Pattern.DOTALL;
				logger.trace("PatternFilter constructed with dotall flag");
			}

			if (flags.contains("u")) {
				flag = flag | Pattern.UNICODE_CASE;
				logger.trace("PatternFilter constructed with unicode case flag");
			}

			if (flags.contains("x")) {
				flag = flag | Pattern.COMMENTS;
				logger.trace("PatternFilter constructed with comments flag");
			}

			if (flags.contains("U")) {
				flag = flag | Pattern.UNICODE_CHARACTER_CLASS;
				logger.trace("PatternFilter constructed with unicode character class flag");
			}

			this.pattern = Pattern.compile(pattern, flag);
			logger.trace("PatternFilter constructed with pattern: {} and flags: {}", pattern, flags);

		} else {
			this.pattern = Pattern.compile(pattern, 0b0);
			logger.trace("PatternFilter constructed with pattern: {} and no flags", pattern);
		}
	}

	private static Literal str(Value argValue, ValueFactory valueFactory) {
		if (argValue instanceof IRI || argValue instanceof Triple) {
			return valueFactory.createLiteral(argValue.toString());
		} else if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
				return literal;
			} else {
				return valueFactory.createLiteral(literal.getLabel());
			}
		} else {
			return null;
		}
	}

	@Override
	boolean checkTuple(Reference t) {
		Value literal = t.get().getValue();
		literal = str(literal, SimpleValueFactory.getInstance());

		if (literal == null)
			return false;

		if (QueryEvaluationUtility.isStringLiteral(literal)) {
			boolean result = pattern.matcher(((Literal) literal).getLabel()).find();
			if (logger.isTraceEnabled()) {
				logger.trace("PatternFilter value: \"{}\" with pattern: \"{}\" and result: {}",
						((Literal) literal).getLabel().replace("\n", "\\n").replace("\"", "\\\""),
						pattern.toString().replace("\n", "\\n").replace("\"", "\\\""), result);
			}
			return result;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("PatternFilter did not match value because value is not a string literal: {}", literal);
		}
		return false;
	}

	@Override
	public String toString() {
		return "PatternFilter{" + "pattern=" + pattern + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		PatternFilter that = (PatternFilter) o;
		return pattern.equals(that.pattern);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), pattern);
	}
}
