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

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.NodeKindFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;

public class NodeKindConstraintComponent extends AbstractSimpleConstraintComponent {

	NodeKind nodeKind;

	public NodeKindConstraintComponent(Resource nodeKind) {
		this.nodeKind = NodeKind.from(nodeKind);
	}

	@Override
	String getSparqlFilterExpression(Variable<Value> variable, boolean negated) {
		if (negated) {
			return "(isIRI(" + variable.asSparqlVariable() + ") && <" + nodeKind.iri + "> IN ( <" + SHACL.IRI + ">, <"
					+ SHACL.BLANK_NODE_OR_IRI + ">, <" + SHACL.IRI_OR_LITERAL + "> ) ) ||\n" +
					"(isLiteral(" + variable.asSparqlVariable() + ") && <" + nodeKind.iri + "> IN ( <" + SHACL.LITERAL
					+ ">, " +
					"<" + SHACL.BLANK_NODE_OR_LITERAL + ">, <" + SHACL.IRI_OR_LITERAL + "> ) ) ||\n" +
					"(isBlank(" + variable.asSparqlVariable() + ") && <" + nodeKind.iri + "> IN ( <" + SHACL.BLANK_NODE
					+ ">, <"
					+ SHACL.BLANK_NODE_OR_IRI + ">, <" + SHACL.BLANK_NODE_OR_LITERAL + "> ) )";
		} else {
			return "!((isIRI(" + variable.asSparqlVariable() + ") && <" + nodeKind.iri + "> IN ( <" + SHACL.IRI + ">, <"
					+ SHACL.BLANK_NODE_OR_IRI + ">, <" + SHACL.IRI_OR_LITERAL + "> ) ) ||\n" +
					"(isLiteral(" + variable.asSparqlVariable() + ") && <" + nodeKind.iri + "> IN ( <" + SHACL.LITERAL
					+ ">, " +
					"<" + SHACL.BLANK_NODE_OR_LITERAL + ">, <" + SHACL.IRI_OR_LITERAL + "> ) ) ||\n" +
					"(isBlank(" + variable.asSparqlVariable() + ") && <" + nodeKind.iri + "> IN ( <" + SHACL.BLANK_NODE
					+ ">, <"
					+ SHACL.BLANK_NODE_OR_IRI + ">, <" + SHACL.BLANK_NODE_OR_LITERAL + "> ) ))";
		}
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.NODE_KIND_PROP, nodeKind.iri);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NodeKindConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new NodeKindConstraintComponent(nodeKind.iri);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new NodeKindFilter(parent, nodeKind);
	}

	public enum NodeKind {

		BlankNode(SHACL.BLANK_NODE),
		IRI(SHACL.IRI),
		Literal(SHACL.LITERAL),
		BlankNodeOrIRI(SHACL.BLANK_NODE_OR_IRI),
		BlankNodeOrLiteral(SHACL.BLANK_NODE_OR_LITERAL),
		IRIOrLiteral(SHACL.IRI_OR_LITERAL),
		;

		private final IRI iri;

		NodeKind(IRI iri) {
			this.iri = iri;
		}

		public static NodeKind from(Resource resource) {
			for (NodeKind value : NodeKind.values()) {
				if (value.iri.equals(resource)) {
					return value;
				}
			}

			throw new IllegalStateException("Unknown nodeKind: " + resource);
		}
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

		NodeKindConstraintComponent that = (NodeKindConstraintComponent) o;

		return nodeKind == that.nodeKind;
	}

	@Override
	public int hashCode() {
		return nodeKind.hashCode() + "NodeKindConstraintComponent".hashCode();
	}
}
