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

import org.eclipse.rdf4j.model.Value;
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
			this.pattern = Pattern.compile(pattern);
			logger.trace("PatternFilter constructed with pattern: {} and no flags", pattern);
		}
	}

	@Override
	boolean checkTuple(Reference t) {
		Value literal = t.get().getValue();

		return pattern.matcher(literal.stringValue()).matches();
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
