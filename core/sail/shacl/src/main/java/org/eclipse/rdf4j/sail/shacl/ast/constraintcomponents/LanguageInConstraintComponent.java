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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LanguageInFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class LanguageInConstraintComponent extends AbstractSimpleConstraintComponent {

	private final List<String> languageIn;
	private final ArrayList<String> languageRanges;
	private final Set<String> lowerCaseLanguageIn;

	public LanguageInConstraintComponent(ShapeSource shapeSource,
			Resource languageIn) {
		super(languageIn);
		this.languageIn = ShaclAstLists.toList(shapeSource, languageIn, Value.class)
				.stream()
				.map(Value::stringValue)
				.collect(Collectors.toList());

		this.languageRanges = new ArrayList<>(new HashSet<>(this.languageIn));

		this.lowerCaseLanguageIn = this.languageIn.stream()
				.filter(l -> !l.contains("*"))
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
	}

	private LanguageInConstraintComponent(LanguageInConstraintComponent languageInConstraintComponent) {
		super(languageInConstraintComponent.getId());
		this.languageIn = languageInConstraintComponent.languageIn;
		this.languageRanges = languageInConstraintComponent.languageRanges;
		this.lowerCaseLanguageIn = languageInConstraintComponent.lowerCaseLanguageIn;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.LANGUAGE_IN, getId());

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(languageIn.stream()
					.map(Values::literal)
					.collect(Collectors.toList()), getId(), model);
		}
	}

	@Override
	String getSparqlFilterExpression(Variable<Value> variable, boolean negated) {
		if (languageRanges.isEmpty()) {
			return "true";
		}

		String filter = languageRanges.stream()
				.map(lang -> "langMatches(lang(" + variable.asSparqlVariable() + "), \"" + lang + "\")")
				.reduce((a, b) -> a + " || " + b)
				.orElseThrow(IllegalStateException::new);

		if (negated) {
			return "(" + filter + ")";
		} else {
			return "!(" + filter + ")";
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.LanguageInConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new LanguageInConstraintComponent(this);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher(ConnectionsGroup connectionsGroup) {
		return (parent) -> new LanguageInFilter(parent, lowerCaseLanguageIn, languageRanges, connectionsGroup);
	}

	@Override
	public String toString() {
		return "LanguageInPropertyShape{" +
				"languageIn=" + Arrays.toString(languageIn.toArray()) +
				", lowerCaseLanguageIn=" + Arrays.toString(lowerCaseLanguageIn.toArray()) +
				", id=" + getId() +
				'}';
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

		LanguageInConstraintComponent that = (LanguageInConstraintComponent) o;

		return languageIn.equals(that.languageIn);
	}

	@Override
	public int hashCode() {
		return languageIn.hashCode() + "LanguageInConstraintComponent".hashCode();
	}
}
