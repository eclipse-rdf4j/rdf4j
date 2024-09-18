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
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class NodeKindFilter extends FilterPlanNode {

	private static final Logger logger = LoggerFactory.getLogger(NodeKindFilter.class);

	private final NodeKindConstraintComponent.NodeKind nodeKind;

	public NodeKindFilter(PlanNode parent, NodeKindConstraintComponent.NodeKind nodeKind,
			ConnectionsGroup connectionsGroup) {
		super(parent, connectionsGroup);
		this.nodeKind = nodeKind;
	}

	@Override
	boolean checkTuple(Reference t) {

		Value value = t.get().getValue();

		switch (nodeKind) {
		case IRI:
			if (value.isIRI()) {
				logger.trace("Tuple accepted because its value is an IRI. Tuple: {}", t);
				return true;
			}
			break;
		case Literal:
			if (value.isLiteral()) {
				logger.trace("Tuple accepted because its value is a Literal. Tuple: {}", t);
				return true;
			}
			break;
		case BlankNode:
			if (value.isBNode()) {
				logger.trace("Tuple accepted because its value is a BlankNode. Tuple: {}", t);
				return true;
			}
			break;
		case IRIOrLiteral:
			if (value.isIRI() || value.isLiteral()) {
				logger.trace("Tuple accepted because its value is an IRI or Literal. Tuple: {}", t);
				return true;
			}
			break;
		case BlankNodeOrIRI:
			if (value.isBNode() || value.isIRI()) {
				logger.trace("Tuple accepted because its value is a BlankNode or IRI. Tuple: {}", t);
				return true;
			}
			break;
		case BlankNodeOrLiteral:
			if (value.isBNode() || value.isLiteral()) {
				logger.trace("Tuple accepted because its value is a BlankNode or Literal. Tuple: {}", t);
				return true;
			}
			break;
		}

		logger.debug("Tuple rejected because its value does not match the expected node kind. Tuple: {}", t);
		return false;
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
