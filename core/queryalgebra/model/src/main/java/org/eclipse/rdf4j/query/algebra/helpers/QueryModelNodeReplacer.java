/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.BinaryValueOperator;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.ValueExpr;

@Deprecated
public class QueryModelNodeReplacer extends AbstractQueryModelVisitor<RuntimeException> {

	private QueryModelNode former;

	private QueryModelNode replacement;

	public void replaceChildNode(QueryModelNode parent, QueryModelNode former, QueryModelNode replacement) {
		this.former = former;
		this.replacement = replacement;
		parent.visit(this);
	}

	public void replaceNode(QueryModelNode former, QueryModelNode replacement) {
		replaceChildNode(former.getParentNode(), former, replacement);
	}

	public void removeChildNode(QueryModelNode parent, QueryModelNode former) {
		replaceChildNode(parent, former, null);
	}

	public void removeNode(QueryModelNode former) {
		replaceChildNode(former.getParentNode(), former, null);
	}

	@Override
	public void meet(Filter node) {
		if (replacement == null) {
			replaceNode(node, node.getArg());
		} else if (replacement instanceof ValueExpr) {
			assert former == node.getCondition();
			node.setCondition((ValueExpr) replacement);
		} else {
			assert former == node.getArg();
			node.setArg((TupleExpr) replacement);
		}
	}

	@Override
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
		if (node.getLeftArg() == former) {
			if (replacement == null) {
				replaceNode(node, node.getRightArg());
			} else {
				node.setLeftArg((TupleExpr) replacement);
			}
		} else {
			assert former == node.getRightArg();
			if (replacement == null) {
				replaceNode(node, node.getLeftArg());
			} else {
				node.setRightArg((TupleExpr) replacement);
			}
		}
	}

	@Override
	protected void meetBinaryValueOperator(BinaryValueOperator node) {
		if (former == node.getLeftArg()) {
			if (replacement == null) {
				replaceNode(node, node.getRightArg());
			} else {
				node.setLeftArg((ValueExpr) replacement);
			}
		} else {
			assert former == node.getRightArg();
			if (replacement == null) {
				replaceNode(node, node.getLeftArg());
			} else {
				node.setRightArg((ValueExpr) replacement);
			}
		}
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
		assert former == node.getArg();
		if (replacement == null) {
			removeNode(node);
		} else {
			node.setArg((TupleExpr) replacement);
		}
	}

	@Override
	protected void meetUnaryValueOperator(UnaryValueOperator node) {
		assert former == node.getArg();
		if (replacement == null) {
			removeNode(node);
		} else {
			node.setArg((ValueExpr) replacement);
		}
	}

	@Override
	protected void meetNode(QueryModelNode node) {
		throw new IllegalArgumentException("Unhandled Node: " + node);
	}
}
