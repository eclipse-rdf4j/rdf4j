/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import org.eclipse.rdf4j.query.parser.serql.ast.ASTEdge;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNode;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNodeElem;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTReifiedStat;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.serql.ast.SyntaxTreeBuilderTreeConstants;
import org.eclipse.rdf4j.query.parser.serql.ast.VisitorException;

/**
 * Inserts anonymous variables into the abstract syntax tree at places where
 * such variables are already implicitly present.
 */
public class AnonymousVarGenerator extends AbstractASTVisitor {

	private int anonymousVarNo = 1;

	@Override
	public Object visit(ASTNode node, Object data)
		throws VisitorException
	{
		if (node.jjtGetNumChildren() == 0) {
			ASTNodeElem nodeElem = createNodeElem();
			nodeElem.jjtSetParent(node);
			node.jjtAppendChild(nodeElem);
		}

		return super.visit(node, data);
	}

	@Override
	public Object visit(ASTReifiedStat node, Object data)
		throws VisitorException
	{
		if (node.jjtGetChild(0) instanceof ASTEdge) {
			// subject node is missing
			ASTNodeElem nodeElem = createNodeElem();
			nodeElem.jjtSetParent(node);
			node.jjtInsertChild(nodeElem, 0);
		}

		if (node.jjtGetNumChildren() <= 2) {
			// object node is missing
			ASTNodeElem nodeElem = createNodeElem();
			nodeElem.jjtSetParent(node);
			node.jjtAppendChild(nodeElem);
		}
		
		if (node.getID() == null) {
			node.setID(createAnonymousVar());
		}

		return super.visit(node, data);
	}

	private ASTNodeElem createNodeElem() {
		ASTNodeElem nodeElem = new ASTNodeElem(SyntaxTreeBuilderTreeConstants.JJTNODEELEM);
		
		ASTVar var = createAnonymousVar();
		var.jjtSetParent(nodeElem);
		nodeElem.jjtAppendChild(var);
		
		return nodeElem;
	}
	
	private ASTVar createAnonymousVar() {
		ASTVar var = new ASTVar(SyntaxTreeBuilderTreeConstants.JJTVAR);
		var.setName("-anon-" + anonymousVarNo++);
		var.setAnonymous(true);
		return var;
	}
}
