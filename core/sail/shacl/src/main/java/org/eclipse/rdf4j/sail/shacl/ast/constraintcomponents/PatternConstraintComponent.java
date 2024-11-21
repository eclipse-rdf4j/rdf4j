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

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PatternFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatternConstraintComponent extends AbstractSimpleConstraintComponent {

	private static final Logger logger = LoggerFactory.getLogger(PatternConstraintComponent.class);

	String pattern;
	String flags;
	private final Pattern compiledPattern;

	public PatternConstraintComponent(String pattern, String flags) {
		super();
		this.pattern = pattern;
		this.flags = flags;

		if (flags == null) {
			this.flags = "";
		}

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

			this.compiledPattern = Pattern.compile(pattern, flag);
			logger.trace("PatternFilter constructed with pattern: {} and flags: {}", pattern, flags);

		} else {
			this.compiledPattern = Pattern.compile(pattern, 0b0);
			logger.trace("PatternFilter constructed with pattern: {} and no flags", pattern);
		}
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.PATTERN, literal(pattern));
		if (flags != null && !flags.isEmpty()) {
			model.add(subject, SHACL.FLAGS, literal(flags));
		}

	}

	@Override
	String getSparqlFilterExpression(Variable<Value> variable, boolean negated) {
		if (negated) {
			return "!isBlank(" + variable.asSparqlVariable() + ") && REGEX(STR(" + variable.asSparqlVariable() + "), \""
					+ escapeRegexForSparql(pattern)
					+ "\", \"" + flags + "\") ";
		} else {
			return " isBlank(" + variable.asSparqlVariable() + ") || !REGEX(STR(" + variable.asSparqlVariable()
					+ "), \"" + escapeRegexForSparql(pattern)
					+ "\", \"" + flags + "\") ";
		}
	}

	private static String escapeRegexForSparql(String pattern) {
		pattern = pattern.replace("\\", "\\\\");
		pattern = pattern.replace("\"", "\\\"");
		pattern = pattern.replace("\n", "\\n");
		return pattern;
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.PatternConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new PatternConstraintComponent(pattern, flags);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher(ConnectionsGroup connectionsGroup) {
		return (parent) -> new PatternFilter(parent, compiledPattern, connectionsGroup);
	}

	@Override
	public List<Literal> getDefaultMessage() {
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		PatternConstraintComponent that = (PatternConstraintComponent) o;

		if (!pattern.equals(that.pattern)) {
			return false;
		}
		return Objects.equals(flags, that.flags);
	}

	@Override
	public int hashCode() {
		int result = pattern.hashCode();
		result = 31 * result + (flags != null ? flags.hashCode() : 0);
		return result + "PatternConstraintComponent".hashCode();
	}
}
