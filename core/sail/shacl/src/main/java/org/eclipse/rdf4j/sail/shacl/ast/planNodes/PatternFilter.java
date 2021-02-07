/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Value;

/**
 * @author Håvard Ottestad
 */
public class PatternFilter extends FilterPlanNode {

	private final Pattern pattern;

	public PatternFilter(PlanNode parent, String pattern, String flags) {
		super(parent);
		if (flags != null && flags.length() > 0) {

			int flag = 0b0;

			if (flags.contains("i")) {
				flag = flag | Pattern.CASE_INSENSITIVE;
			}

			if (flags.contains("d")) {
				flag = flag | Pattern.UNIX_LINES;
			}

			if (flags.contains("m")) {
				flag = flag | Pattern.MULTILINE;
			}

			if (flags.contains("s")) {
				flag = flag | Pattern.DOTALL;
			}

			if (flags.contains("u")) {
				flag = flag | Pattern.UNICODE_CASE;
			}

			if (flags.contains("x")) {
				flag = flag | Pattern.COMMENTS;
			}

			if (flags.contains("U")) {
				flag = flag | Pattern.UNICODE_CHARACTER_CLASS;
			}

			this.pattern = Pattern.compile(pattern, flag);

		} else {
			this.pattern = Pattern.compile(pattern);

		}

	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		Value literal = t.getValue();

		return pattern.matcher(literal.stringValue()).matches();
	}

	@Override
	public String toString() {
		return "PatternFilter{" + "pattern=" + pattern + '}';
	}
}
