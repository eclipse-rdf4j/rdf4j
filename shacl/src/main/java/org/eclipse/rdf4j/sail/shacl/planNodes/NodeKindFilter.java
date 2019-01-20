/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/


package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.AST.NodeKindPropertyShape;

/**
 * @author Håvard Ottestad
 */
public class NodeKindFilter extends FilterPlanNode {

	private final NodeKindPropertyShape.NodeKind  nodeKind;

	public NodeKindFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, NodeKindPropertyShape.NodeKind nodeKind) {
		super(parent, trueNode, falseNode);
		this.nodeKind = nodeKind;
	}

	@Override
	boolean checkTuple(Tuple t) {

		Value value = t.line.get(1);
/*
		BlankNode(SHACL.BLANK_NODE),
			IRI(SHACL.IRI),
			Literal(SHACL.LITERAL),
			BlankNodeOrIRI(SHACL.BLANK_NODE_OR_IRI),
			BlankNodeOrLiteral(SHACL.BLANK_NODE_OR_LITERAL),
			IRIOrLiteral(SHACL.IRI_OR_LITERAL),
*/



	switch (nodeKind) {
		case IRI:
			return value instanceof IRI;
		case Literal:
			return value instanceof  Literal;
		case BlankNode:
			return value instanceof BNode;
		case IRIOrLiteral:
			return value instanceof IRI || value instanceof Literal;
		case BlankNodeOrIRI:
			return value instanceof BNode || value instanceof  IRI;
		case BlankNodeOrLiteral:
			return value instanceof BNode || value instanceof  Literal;
	}

	throw new IllegalStateException("Unknown nodeKind");

	}


	@Override
	public String toString() {
		return "NodeKindFilter{" +
			"nodeKind=" + nodeKind +
			'}';
	}
}
