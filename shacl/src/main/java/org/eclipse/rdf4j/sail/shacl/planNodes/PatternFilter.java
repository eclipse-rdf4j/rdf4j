/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Value;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Håvard Ottestad
 */
public class PatternFilter extends FilterPlanNode {

	private final Pattern pattern;

	public PatternFilter(PlanNode parent, String pattern, Optional<String> flags) {
		super(parent);
		if (flags.isPresent()) {

			int flag = 0b0;

			String flagsString = flags.get();

			if (flagsString.contains("i")) {
				flag = flag | Pattern.CASE_INSENSITIVE;
			}

			if (flagsString.contains("d")) {
				flag = flag | Pattern.UNIX_LINES;
			}

			if (flagsString.contains("m")) {
				flag = flag | Pattern.MULTILINE;
			}

			if (flagsString.contains("s")) {
				flag = flag | Pattern.DOTALL;
			}

			if (flagsString.contains("u")) {
				flag = flag | Pattern.UNICODE_CASE;
			}

			if (flagsString.contains("x")) {
				flag = flag | Pattern.COMMENTS;
			}

			if (flagsString.contains("U")) {
				flag = flag | Pattern.UNICODE_CHARACTER_CLASS;
			}

			this.pattern = Pattern.compile(pattern, flag);

		} else {
			this.pattern = Pattern.compile(pattern);

		}

	}

	@Override
	boolean checkTuple(Tuple t) {
		Value literal = t.line.get(1);

		return pattern.matcher(literal.stringValue()).matches();
	}

	@Override
	public String toString() {
		return "PatternFilter{" + "pattern=" + pattern + '}';
	}
}
