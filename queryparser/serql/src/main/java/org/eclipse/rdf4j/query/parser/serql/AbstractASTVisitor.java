/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import org.eclipse.rdf4j.query.parser.serql.ast.ASTAnd;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTArgList;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTBNode;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTBasicPathExpr;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTBasicPathExprTail;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTBooleanConstant;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTBound;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTCompOperator;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTCompare;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTCompareAll;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTCompareAny;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTConstruct;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTConstructQuery;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTDatatype;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTEdge;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTExists;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTFrom;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTFunctionCall;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTGraphIntersect;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTGraphMinus;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTGraphUnion;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTIn;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTInList;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTIsBNode;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTIsLiteral;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTIsResource;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTIsURI;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLabel;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLang;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLangMatches;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLike;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLimit;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLiteral;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLocalName;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNamespace;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNamespaceDecl;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNode;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNodeElem;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNot;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTNull;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTOffset;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTOptPathExpr;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTOptPathExprTail;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTOr;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTOrderBy;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTOrderExpr;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTPathExprList;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTPathExprUnion;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTProjectionElem;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTQName;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTQueryBody;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTRegex;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTReifiedStat;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTSameTerm;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTSelect;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTSelectQuery;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTStr;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTString;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTTupleIntersect;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTTupleMinus;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTTupleUnion;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTURI;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTWhere;
import org.eclipse.rdf4j.query.parser.serql.ast.SimpleNode;
import org.eclipse.rdf4j.query.parser.serql.ast.SyntaxTreeBuilderVisitor;
import org.eclipse.rdf4j.query.parser.serql.ast.VisitorException;

public abstract class AbstractASTVisitor implements SyntaxTreeBuilderVisitor {

	@Override
	public Object visit(SimpleNode node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTQueryContainer node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNamespaceDecl node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTupleUnion node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTupleMinus node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTupleIntersect node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphUnion node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphMinus node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphIntersect node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSelectQuery node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSelect node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTProjectionElem node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConstructQuery node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConstruct node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTQueryBody node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTFrom node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTWhere node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOrderBy node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOrderExpr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLimit node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOffset node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathExprList node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathExprUnion node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBasicPathExpr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOptPathExpr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBasicPathExprTail node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOptPathExprTail node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTEdge node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNodeElem node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNode node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTReifiedStat node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTAnd node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBooleanConstant node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNot node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBound node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsResource node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsLiteral node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsURI node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsBNode node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLangMatches node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTExists node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSameTerm node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCompare node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCompareAny node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCompareAll node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLike node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTRegex node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIn node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTInList node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCompOperator node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTVar node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDatatype node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLang node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLabel node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNamespace node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLocalName node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTFunctionCall node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTArgList node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTURI node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTQName node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBNode node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLiteral node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTString node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNull node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}
}
