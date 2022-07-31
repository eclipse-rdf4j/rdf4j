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

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PatternFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;

public class PatternConstraintComponent extends SimpleAbstractConstraintComponent {

	String pattern;
	String flags;

	public PatternConstraintComponent(String pattern, String flags) {
		this.pattern = pattern;
		this.flags = flags;

		if (flags == null) {
			this.flags = "";
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
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return "!isBlank(?" + varName + ") && REGEX(STR(?" + varName + "), \"" + escapeRegexForSparql(pattern)
					+ "\", \"" + flags + "\") ";
		} else {
			return " isBlank(?" + varName + ") || !REGEX(STR(?" + varName + "), \"" + escapeRegexForSparql(pattern)
					+ "\", \"" + flags + "\") ";
		}
	}

	private static String escapeRegexForSparql(String pattern) {
		return pattern.replace("\\", "\\\\");
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
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new PatternFilter(parent, pattern, flags);
	}
}
