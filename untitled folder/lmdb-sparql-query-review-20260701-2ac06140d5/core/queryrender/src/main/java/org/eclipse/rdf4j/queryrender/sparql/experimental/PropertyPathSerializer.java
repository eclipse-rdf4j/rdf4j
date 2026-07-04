/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.util.Stack;

import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.queryrender.RenderUtils;

class PropertyPathSerializer extends AbstractQueryModelVisitor<RuntimeException> {

	private StringBuilder builder;
	private final Stack<VarInfo> currentSubjectVarStack = new Stack<>();
	private AbstractSerializableParsedQuery currentQueryProfile;

	public String serialize(ArbitraryLengthPath path, AbstractSerializableParsedQuery currentQueryProfile) {
		this.builder = new StringBuilder();
		this.currentQueryProfile = currentQueryProfile;

		Var subjVar = path.getSubjectVar();
		Var objVar = path.getObjectVar();

		if (path.getContextVar() != null) {
			builder.append("GRAPH ");
			path.getContextVar().visit(this);
			builder.append(" {");
		}
		subjVar.visit(this);
		builder.append(" ");
		path.visit(this);

		builder.append(" ");
		objVar.visit(this);
		builder.append(" .");
		if (path.getContextVar() != null) {
			builder.append(" }");
		}
		builder.append(" \n");
		return this.builder.toString().trim();
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws RuntimeException {
		currentSubjectVarStack.push(new VarInfo(node.getSubjectVar(), false));
		builder.append("(");
		node.getPathExpression().visit(this);

		while (!currentSubjectVarStack.isEmpty()) {
			VarInfo currentSubjectVar = currentSubjectVarStack.pop();
			if (currentSubjectVar.inverse) {
				builder.append(")");
			}
		}

		builder.append(")");
		if (node.getMinLength() == 0) {
			builder.append("*");
		} else {
			builder.append("+");
		}
		// super.meet(node);
	}

	@Override
	public void meet(Join node) throws RuntimeException {
		builder.append("(");
		node.getLeftArg().visit(this);
		builder.append("/");
		node.getRightArg().visit(this);
		builder.append(")");
	}

	@Override
	public void meet(Union node) throws RuntimeException {
		VarInfo currentSubjectVar = currentSubjectVarStack.peek();
		boolean containsZeroLength = (node.getLeftArg() instanceof ZeroLengthPath);
		if (!containsZeroLength) {
			builder.append("(");
		}
		currentSubjectVarStack.push(currentSubjectVar);
		VarInfo currentObjectVarLeft, currentObjectVarRight;
		node.getLeftArg().visit(this);
		currentObjectVarLeft = currentSubjectVarStack.pop();
		if (!containsZeroLength) {
			builder.append("|");
		}
		currentSubjectVarStack.push(currentSubjectVar);
		node.getRightArg().visit(this);
		currentObjectVarRight = currentSubjectVarStack.pop();

		if (currentObjectVarRight.inverse) {
			builder.append(")");
		}
		if (!containsZeroLength) {
			builder.append(")");
		} else {
			builder.append("?");
		}
		currentSubjectVarStack.push(currentObjectVarLeft);
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		VarInfo subjVar = currentSubjectVarStack.peek();

		Var predicate = node.getPredicateVar();
		if (subjVar.var.getName().equals(node.getObjectVar().getName())) {
			// => push inverse marker to stack
			builder.append("^(");
			currentSubjectVarStack.push(new VarInfo(node.getSubjectVar(), true));
		} else {
			currentSubjectVarStack.push(new VarInfo(node.getObjectVar(), false));
		}

		if (predicate.hasValue()) {
			builder.append(RenderUtils.toSPARQL(node.getPredicateVar().getValue()));
		} else {
			builder.append("?");
			builder.append(predicate.getName());
		}
	}

	@Override
	public void meet(Var node) throws RuntimeException {
		if (node.hasValue()) {
			builder.append(RenderUtils.toSPARQL(node.getValue()));
		} else {
			if (node.isAnonymous()) {
				if (currentQueryProfile.extensionElements.containsKey(node.getName())) {
					ExtensionElem elem = currentQueryProfile.extensionElements.get(node.getName());
					elem.getExpr().visit(this);
				} else {
					builder.append("_:");
					builder.append(node.getName());
				}
			} else {
				builder.append("?");
				builder.append(node.getName());
			}
		}

		super.meet(node);
	}

	@Override
	public void meet(ZeroLengthPath node) throws RuntimeException {
		VarInfo subjVar = currentSubjectVarStack.pop();
		if (subjVar.inverse) {
			builder.append(")");
		}
		currentSubjectVarStack.push(new VarInfo(node.getObjectVar(), false));

	}

	static class VarInfo {
		Var var;
		boolean inverse;

		VarInfo(Var var, boolean inverse) {
			super();
			this.var = var;
			this.inverse = inverse;
		}
	}
}
