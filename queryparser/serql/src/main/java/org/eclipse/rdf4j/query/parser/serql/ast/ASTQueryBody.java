/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

import java.util.ArrayList;
import java.util.List;

public class ASTQueryBody extends SimpleNode {

	public ASTQueryBody(int id) {
		super(id);
	}

	public ASTQueryBody(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public List<ASTFrom> getFromClauseList() {
		List<ASTFrom> fromClauseList = new ArrayList<>(children.size());

		for (Node n : children) {
			if (n instanceof ASTFrom) {
				fromClauseList.add((ASTFrom) n);
			} else {
				break;
			}
		}

		return fromClauseList;
	}

	public boolean hasWhereClause() {
		return getWhereClause() != null;
	}

	public ASTWhere getWhereClause() {
		for (Node n : children) {
			if (n instanceof ASTWhere) {
				return (ASTWhere) n;
			}
		}

		return null;
	}
}
