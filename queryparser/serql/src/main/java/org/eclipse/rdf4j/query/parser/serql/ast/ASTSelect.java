/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

import java.util.List;

public class ASTSelect extends SimpleNode {

	private boolean distinct = false;

	private boolean reduced = false;

	private boolean wildcard = false;

	public ASTSelect(int id) {
		super(id);
	}

	public ASTSelect(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setReduced(boolean reduced) {
		this.reduced = reduced;
	}

	public boolean isReduced() {
		return reduced;
	}

	public boolean isWildcard() {
		return wildcard;
	}

	public void setWildcard(boolean wildcard) {
		this.wildcard = wildcard;
	}

	public List<ASTProjectionElem> getProjectionElemList() {
		return new CastingList<ASTProjectionElem>(children);
	}

	@Override
	public String toString() {
		String result = super.toString();

		if (distinct || reduced || wildcard) {
			result += " (";
			if (distinct) {
				result += " distinct";
			}
			if (reduced) {
				result += " reduced";
			}
			if (wildcard) {
				result += " *";
			}
			result += " )";
		}

		return result;
	}
}
