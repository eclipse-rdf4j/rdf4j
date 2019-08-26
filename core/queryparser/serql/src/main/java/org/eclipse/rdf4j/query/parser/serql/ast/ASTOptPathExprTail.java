/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTOptPathExprTail extends ASTPathExprTail {

	public ASTOptPathExprTail(int id) {
		super(id);
	}

	public ASTOptPathExprTail(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	/**
	 * Gets the optional tail part of the path expression.
	 * 
	 * @return The optional tail part of the path expression.
	 */
	public ASTBasicPathExprTail getOptionalTail() {
		return (ASTBasicPathExprTail) children.get(0);
	}

	public boolean hasWhereClause() {
		return getWhereClause() != null;
	}

	/**
	 * Gets the where-clause that constrains the results of the optional path expression tail, if any.
	 * 
	 * @return The where-clause, or <tt>null</tt> if not available.
	 */
	public ASTWhere getWhereClause() {
		if (children.size() >= 2) {
			Node node = children.get(1);

			if (node instanceof ASTWhere) {
				return (ASTWhere) node;
			}
		}

		return null;
	}

	@Override
	public ASTPathExprTail getNextTail() {
		if (children.size() >= 2) {
			Node node = children.get(children.size() - 1);

			if (node instanceof ASTPathExprTail) {
				return (ASTPathExprTail) node;
			}
		}

		return null;
	}
}
