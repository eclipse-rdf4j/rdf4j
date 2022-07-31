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
package org.eclipse.rdf4j.queryrender.sparql;

import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BinaryValueOperator;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.CompareAll;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Label;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Namespace;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.queryrender.BaseTupleExprRenderer;
import org.eclipse.rdf4j.queryrender.RenderUtils;

/**
 * <p>
 * Renders a {@link ValueExpr} into SPARQL syntax.
 * </p>
 *
 * @author Michael Grove
 */
final class SparqlValueExprRenderer extends AbstractQueryModelVisitor<Exception> {

	/**
	 * The current rendered value
	 */
	private StringBuffer mBuffer = new StringBuffer();

	/**
	 * Reset the state of this renderer
	 */
	public void reset() {
		mBuffer = new StringBuffer();
	}

	/**
	 * Return the rendering of the ValueExpr object
	 *
	 * @param theExpr the expression to render
	 * @return the rendering
	 * @throws Exception if there is an error while rendering
	 */
	public String render(ValueExpr theExpr) throws Exception {
		theExpr.visit(this);

		return mBuffer.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Bound theOp) throws Exception {
		mBuffer.append(" bound(");
		theOp.getArg().visit(this);
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Var theVar) throws Exception {
		if (theVar.isAnonymous() && !theVar.hasValue()) {
			mBuffer.append("?").append(BaseTupleExprRenderer.scrubVarName(theVar.getName()));
		} else if (theVar.hasValue()) {
			mBuffer.append(RenderUtils.toSPARQL(theVar.getValue()));
		} else {
			mBuffer.append("?").append(theVar.getName());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(BNodeGenerator theGen) throws Exception {
		mBuffer.append(theGen.getSignature());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(MathExpr theOp) throws Exception {
		mBuffer.append("(");
		theOp.getLeftArg().visit(this);
		mBuffer.append(" ").append(theOp.getOperator().getSymbol()).append(" ");
		theOp.getRightArg().visit(this);
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Compare theOp) throws Exception {
		mBuffer.append("(");
		theOp.getLeftArg().visit(this);
		mBuffer.append(" ").append(theOp.getOperator().getSymbol()).append(" ");
		theOp.getRightArg().visit(this);
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Exists theOp) throws Exception {
		mBuffer.append(" exists(");
		mBuffer.append(renderTupleExpr(theOp.getSubQuery()));
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(In theOp) throws Exception {
		theOp.getArg().visit(this);
		mBuffer.append(" in ");
		mBuffer.append("(");
		mBuffer.append(renderTupleExpr(theOp.getSubQuery()));
		mBuffer.append(")");
	}

	/**
	 * Renders the tuple expression as a query string.
	 *
	 * @param theExpr the expr to render
	 * @return the rendered expression
	 * @throws Exception if there is an error while rendering
	 */
	private String renderTupleExpr(TupleExpr theExpr) throws Exception {
		SparqlTupleExprRenderer aRenderer = new SparqlTupleExprRenderer();
		return aRenderer.render(theExpr);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(CompareAll theOp) throws Exception {
		mBuffer.append("(");
		theOp.getArg().visit(this);
		mBuffer.append(" ").append(theOp.getOperator().getSymbol()).append(" all ");
		mBuffer.append("(");
		mBuffer.append(renderTupleExpr(theOp.getSubQuery()));
		mBuffer.append(")");
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(ValueConstant theVal) throws Exception {
		mBuffer.append(RenderUtils.toSPARQL(theVal.getValue()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(FunctionCall theOp) throws Exception {
		mBuffer.append(knownFunctionsUriReduction(theOp.getURI())).append("(");

		boolean aFirst = true;
		for (ValueExpr aArg : theOp.getArgs()) {
			if (!aFirst) {
				mBuffer.append(", ");
			} else {
				aFirst = false;
			}

			aArg.visit(this);
		}
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(CompareAny theOp) throws Exception {
		mBuffer.append("(");
		theOp.getArg().visit(this);
		mBuffer.append(" ").append(theOp.getOperator().getSymbol()).append(" any ");
		mBuffer.append("(");
		mBuffer.append(renderTupleExpr(theOp.getSubQuery()));
		mBuffer.append(")");
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Regex theOp) throws Exception {
		mBuffer.append(" regex(");
		theOp.getArg().visit(this);
		mBuffer.append(", ");
		theOp.getPatternArg().visit(this);
		if (theOp.getFlagsArg() != null) {
			mBuffer.append(",");
			theOp.getFlagsArg().visit(this);
		}
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(LangMatches theOp) throws Exception {
		mBuffer.append(" langMatches(");
		theOp.getLeftArg().visit(this);
		mBuffer.append(", ");
		theOp.getRightArg().visit(this);
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(SameTerm theOp) throws Exception {
		mBuffer.append(" sameTerm(");
		theOp.getLeftArg().visit(this);
		mBuffer.append(", ");
		theOp.getRightArg().visit(this);
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(And theAnd) throws Exception {
		binaryMeet("&&", theAnd);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Or theOr) throws Exception {
		binaryMeet("||", theOr);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Not theNot) throws Exception {
		mBuffer.append("(");
		unaryMeet("!", theNot);
		mBuffer.append(")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Count theOp) throws Exception {
		unaryMeet("count", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Datatype theOp) throws Exception {
		unaryMeet("datatype", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(IsBNode theOp) throws Exception {
		unaryMeet("isBlank", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(IsLiteral theOp) throws Exception {
		unaryMeet("isLiteral", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(IsNumeric theOp) throws Exception {
		unaryMeet("isNumeric", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(IsResource theOp) throws Exception {
		// there's no isResource method in SPARQL, so lets serialize this as not
		// isLiteral -- if something is not a literal
		// then its probably a resource, tho it might be just not bound so this
		// might not be 100% correct,
		// but close enough for right now.
		unaryMeet("!isLiteral", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(IsURI theOp) throws Exception {
		unaryMeet("isURI", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(IRIFunction theOp) throws Exception {
		unaryMeet("IRI", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Label theOp) throws Exception {
		unaryMeet("label", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Lang theOp) throws Exception {
		unaryMeet("lang", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(LocalName theOp) throws Exception {
		unaryMeet("localName", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Min theOp) throws Exception {
		unaryMeet("min", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Max theOp) throws Exception {
		unaryMeet("max", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Namespace theOp) throws Exception {
		unaryMeet("namespace", theOp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void meet(Str theOp) throws Exception {
		unaryMeet("str", theOp);
	}

	private void binaryMeet(String theOpStr, BinaryValueOperator theOp) throws Exception {
		mBuffer.append(" (");
		theOp.getLeftArg().visit(this);
		mBuffer.append(" ").append(theOpStr).append(" ");
		theOp.getRightArg().visit(this);
		mBuffer.append(")");
	}

	private void unaryMeet(String theOpStr, UnaryValueOperator theOp) throws Exception {
		mBuffer.append(" ").append(theOpStr).append("(");
		theOp.getArg().visit(this);
		mBuffer.append(")");
	}

	private String knownFunctionsUriReduction(String functionUri) {
		switch (functionUri.toLowerCase()) {
		// Functional Forms

		// Functions on RDF Terms
		case "strdt":
			return "strdt";

		case "strlang":
			return "strlang";

		case "uuid":
			return "uuid";

		case "struuid":
			return "struuid";

		// Functions on Strings
		case "http://www.w3.org/2005/xpath-functions#string-length":
			return "strlen";

		case "http://www.w3.org/2005/xpath-functions#substring":
			return "substr";

		case "http://www.w3.org/2005/xpath-functions#upper-case":
			return "ucase";

		case "http://www.w3.org/2005/xpath-functions#lower-case":
			return "lcase";

		case "http://www.w3.org/2005/xpath-functions#starts-with":
			return "strstarts";

		case "http://www.w3.org/2005/xpath-functions#ends-with":
			return "strends";

		case "http://www.w3.org/2005/xpath-functions#contains":
			return "contains";

		case "http://www.w3.org/2005/xpath-functions#substring-before":
			return "strbefore";

		case "http://www.w3.org/2005/xpath-functions#substring-after":
			return "strafter";

		case "http://www.w3.org/2005/xpath-functions#encode-for-uri":
			return "encode_for_uri";

		case "http://www.w3.org/2005/xpath-functions#concat":
			return "concat";

		case "http://www.w3.org/2005/xpath-functions#matches":
			return "regex";

		case "http://www.w3.org/2005/xpath-functions#replace":
			return "replace";

		// Functions on Numerics
		case "http://www.w3.org/2005/xpath-functions#numeric-abs":
			return "abs";

		case "http://www.w3.org/2005/xpath-functions#numeric-round":
			return "round";

		case "http://www.w3.org/2005/xpath-functions#numeric-ceil":
			return "ceil";

		case "http://www.w3.org/2005/xpath-functions#numeric-floor":
			return "floor";

		case "rand":
			return "rand";

		// Functions on Dates and Times
		case "now":
			return "now";

		case "http://www.w3.org/2005/xpath-functions#year-from-datetime":
			return "year";

		case "http://www.w3.org/2005/xpath-functions#month-from-datetime":
			return "month";

		case "http://www.w3.org/2005/xpath-functions#day-from-datetime":
			return "day";

		case "http://www.w3.org/2005/xpath-functions#hours-from-datetime":
			return "hours";

		case "http://www.w3.org/2005/xpath-functions#minutes-from-datetime":
			return "minutes";

		case "http://www.w3.org/2005/xpath-functions#seconds-from-datetime":
			return "seconds";

		case "http://www.w3.org/2005/xpath-functions#timezone-from-datetime":
			return "timezone";

		case "tz":
			return "tz";

		// Hash Functions
		case "md5":
			return "md5";

		case "sha1":
			return "sha1";

		case "sha256":
			return "sha256";

		case "sha384":
			return "sha384";

		case "sha512":
			return "sha512";

		default:
			return "<" + functionUri + ">";
		}
	}
}
