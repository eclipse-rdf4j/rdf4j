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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.NodeKindConstraintComponent;

/**
 * @author Håvard Ottestad
 */
public class NodeKindFilter extends FilterPlanNode {

	private final NodeKindConstraintComponent.NodeKind nodeKind;

	public NodeKindFilter(PlanNode parent, NodeKindConstraintComponent.NodeKind nodeKind) {
		super(parent);
		this.nodeKind = nodeKind;
	}

	@Override
	boolean checkTuple(ValidationTuple t) {

		Value value = t.getValue();
		/*
		 * BlankNode(SHACL.BLANK_NODE), IRI(SHACL.IRI), Literal(SHACL.LITERAL), BlankNodeOrIRI(SHACL.BLANK_NODE_OR_IRI),
		 * BlankNodeOrLiteral(SHACL.BLANK_NODE_OR_LITERAL), IRIOrLiteral(SHACL.IRI_OR_LITERAL),
		 */

		switch (nodeKind) {
		case IRI:
			return value.isIRI();
		case Literal:
			return value.isLiteral();
		case BlankNode:
			return value.isBNode();
		case IRIOrLiteral:
			return value.isIRI() || value.isLiteral();
		case BlankNodeOrIRI:
			return value.isBNode() || value.isIRI();
		case BlankNodeOrLiteral:
			return value.isBNode() || value.isLiteral();
		}

		throw new IllegalStateException("Unknown nodeKind");

	}

	@Override
	public String toString() {
		return "NodeKindFilter{" + "nodeKind=" + nodeKind + '}';
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
		NodeKindFilter that = (NodeKindFilter) o;
		return nodeKind == that.nodeKind;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), nodeKind);
	}
}
