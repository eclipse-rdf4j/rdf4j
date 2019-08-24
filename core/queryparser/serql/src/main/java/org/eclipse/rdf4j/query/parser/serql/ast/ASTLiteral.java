/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTLiteral extends ASTValue {

	private String label;

	private String lang;

	public ASTLiteral(int id) {
		super(id);
	}

	public ASTLiteral(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLang() {
		return lang;
	}

	public boolean hasLang() {
		return lang != null;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public ASTValueExpr getDatatypeNode() {
		if (children.size() >= 1) {
			return (ASTValueExpr) children.get(0);
		}

		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(super.toString());

		sb.append(" (\"").append(label).append("\"");

		if (lang != null) {
			sb.append('@').append(lang);
		}

		sb.append(")");

		return sb.toString();
	}
}
