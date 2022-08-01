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
package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAbs;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAdd;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAnd;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAskQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAvg;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBNodeFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBaseDecl;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBasicGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBind;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBindingValue;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBindingsClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBlankNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBlankNodePropertyList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBound;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCeil;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTClear;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCoalesce;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCollection;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCompare;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConcat;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstTripleRef;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstraint;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstruct;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstructQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTContains;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCopy;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCount;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCreate;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDatasetClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDatatype;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDay;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteWhere;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDescribe;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDescribeQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDrop;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTEncodeForURI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTExistsFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTFalse;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTFloor;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTFunctionCall;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphOrDefault;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphPatternGroup;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphRefAll;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGroupClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGroupConcat;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGroupCondition;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTHavingClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTHours;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRIFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIf;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIn;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInfix;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInlineData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInsertClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInsertData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsBlank;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsLiteral;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsNumeric;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLang;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLangMatches;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLimit;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLoad;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLowerCase;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMD5;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMath;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMax;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMin;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMinusGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMinutes;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTModify;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMonth;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMove;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNot;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNotExistsFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNotIn;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNow;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNumericLiteral;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTObjectList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOffset;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOptionalGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOr;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOrderClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOrderCondition;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathAlternative;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathElt;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathMod;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathOneInPropertySet;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathSequence;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPrefixDecl;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTProjectionElem;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPropertyList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPropertyListPath;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQName;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQuadsNotTriples;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRDFLiteral;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRand;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRegexExpression;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTReplace;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRound;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA1;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA224;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA256;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA384;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA512;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSTRUUID;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSameTerm;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSample;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSeconds;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelect;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelectQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTServiceGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStr;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrAfter;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrBefore;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrDt;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrEnds;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrLang;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrLen;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrStarts;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTString;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSubstr;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSum;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTimezone;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTripleRef;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTriplesSameSubject;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTriplesSameSubjectPath;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTrue;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTz;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUUID;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUnionGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUnparsedQuadDataBlock;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpdate;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpdateContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpdateSequence;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpperCase;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTWhereClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTYear;
import org.eclipse.rdf4j.query.parser.sparql.ast.SimpleNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilderVisitor;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * Base class for visitors of the SPARQL AST.
 *
 * @author arjohn
 */
public abstract class AbstractASTVisitor implements SyntaxTreeBuilderVisitor {

	@Override
	public Object visit(ASTAbs node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTUpdateSequence node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBindingValue node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTInlineData node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTUnparsedQuadDataBlock node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTUpdateContainer node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTAdd node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBindingSet node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTClear node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCopy node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCreate node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDeleteClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDeleteData node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDeleteWhere node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDrop node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphOrDefault node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphRefAll node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTInfix node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTInsertClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTInsertData node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLoad node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTModify node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMove node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNow node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTYear node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMonth node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDay node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTHours node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTz node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMinutes node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSeconds node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTimezone node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTAnd node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTAskQuery node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTAvg node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMD5 node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSHA1 node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSHA224 node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSHA256 node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSHA384 node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSHA512 node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBaseDecl node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBasicGraphPattern node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBind node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBindingsClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBlankNode node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBlankNodePropertyList node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBNodeFunc node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTBound node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCeil node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCoalesce node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConcat node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTContains node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCollection node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCompare node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConstraint node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConstruct node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConstructQuery node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTCount node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDatasetClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDatatype node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDescribe node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTDescribeQuery node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTExistsFunc node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTEncodeForURI node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTFalse node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTFloor node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTFunctionCall node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphGraphPattern node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGraphPatternGroup node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGroupClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGroupConcat node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTGroupCondition node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTHavingClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIf node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIn node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIRI node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIRIFunc node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsBlank node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsIRI node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsLiteral node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTIsNumeric node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLang node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLangMatches node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLimit node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTLowerCase node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMath node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMax node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMin node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTMinusGraphPattern node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNot node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNotExistsFunc node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNotIn node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTNumericLiteral node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTObjectList node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOffset node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOptionalGraphPattern node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOrderClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTOrderCondition node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathAlternative node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathElt node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathMod node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathOneInPropertySet node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPathSequence node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPrefixDecl node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTProjectionElem node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPropertyList node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTPropertyListPath node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTQName node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTQueryContainer node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTRand node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTRDFLiteral node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTRegexExpression node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTReplace node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTRound node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSameTerm node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSample node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSelect node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSelectQuery node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTServiceGraphPattern node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrAfter node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrBefore node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrDt node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrEnds node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTString node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTUUID node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSTRUUID node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrLang node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrLen node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTStrStarts node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSubstr node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTSum node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTriplesSameSubject node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTriplesSameSubjectPath node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTQuadsNotTriples node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTrue node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTUnionGraphPattern node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	public Object visit(ASTUpdate node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTUpperCase node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTVar node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTWhereClause node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(SimpleNode node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTTripleRef node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}

	@Override
	public Object visit(ASTConstTripleRef node, Object data) throws VisitorException {
		return node.childrenAccept(this, data);
	}
}
