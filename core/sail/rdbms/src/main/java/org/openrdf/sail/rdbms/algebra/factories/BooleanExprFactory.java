/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.algebra.factories;

import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.abs;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.and;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.cmp;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.concat;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.eq;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.eqComparingNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.eqIfNotNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.gt;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.isNotNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.isNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.like;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.lowercase;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.neq;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.not;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.num;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.or;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.regex;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.simple;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.sqlNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.str;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.sub;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.unsupported;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.And;
import org.openrdf.query.algebra.Bound;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.IsBNode;
import org.openrdf.query.algebra.IsLiteral;
import org.openrdf.query.algebra.IsResource;
import org.openrdf.query.algebra.IsURI;
import org.openrdf.query.algebra.LangMatches;
import org.openrdf.query.algebra.Not;
import org.openrdf.query.algebra.Or;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.Regex;
import org.openrdf.query.algebra.SameTerm;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.Compare.CompareOp;
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;
import org.openrdf.sail.rdbms.algebra.FalseValue;
import org.openrdf.sail.rdbms.algebra.RefIdColumn;
import org.openrdf.sail.rdbms.algebra.SqlCase;
import org.openrdf.sail.rdbms.algebra.SqlNull;
import org.openrdf.sail.rdbms.algebra.TrueValue;
import org.openrdf.sail.rdbms.algebra.base.SqlExpr;
import org.openrdf.sail.rdbms.exceptions.UnsupportedRdbmsOperatorException;

/**
 * Boolean SQL expression factory. This factory can convert a number of core
 * algebra nodes into an SQL expression.
 * 
 * @author James Leigh
 * 
 */
public class BooleanExprFactory extends AbstractQueryModelVisitor<UnsupportedRdbmsOperatorException> {

	private static final double HR14 = 14 * 60 * 60 * 1000;

	protected SqlExpr result;

	private SqlExprFactory sql;

	public SqlExpr createBooleanExpr(ValueExpr expr)
		throws UnsupportedRdbmsOperatorException
	{
		result = null;
		if (expr == null)
			return new SqlNull();
		expr.visit(this);
		if (result == null)
			return new SqlNull();
		return result;
	}

	@Override
	public void meet(And node)
		throws UnsupportedRdbmsOperatorException
	{
		result = and(bool(node.getLeftArg()), bool(node.getRightArg()));
	}

	@Override
	public void meet(Bound node)
		throws UnsupportedRdbmsOperatorException
	{
		result = not(isNull(new RefIdColumn(node.getArg())));
	}

	@Override
	public void meet(Compare compare)
		throws UnsupportedRdbmsOperatorException
	{
		ValueExpr left = compare.getLeftArg();
		ValueExpr right = compare.getRightArg();
		CompareOp op = compare.getOperator();
		switch (op) {
			case EQ:
				if (isTerm(left) && isTerm(right)) {
					result = termsEqual(left, right);
				}
				else {
					result = equal(left, right);
				}
				break;
			case NE:
				if (isTerm(left) && isTerm(right)) {
					result = not(termsEqual(left, right));
				}
				else {
					result = not(equal(left, right));
				}
				break;
			case GE:
			case GT:
			case LE:
			case LT:
				SqlExpr simple = and(simple(type(left)), simple(type(right)));
				SqlExpr labels = and(cmp(label(left), op, label(right)), simple);
				SqlExpr time = cmp(time(left), op, time(right));
				SqlExpr within = cmp(time(left), op, sub(time(right), num(HR14)));
				SqlExpr comp = or(eq(zoned(left), zoned(right)), within);
				SqlExpr dateTime = and(eq(type(left), type(right)), and(comp, time));
				result = or(cmp(numeric(left), op, numeric(right)), or(dateTime, labels));
				break;
		}
	}

	@Override
	public void meet(IsBNode node)
		throws UnsupportedRdbmsOperatorException
	{
		result = isNotNull(sql.createBNodeExpr(node.getArg()));
	}

	@Override
	public void meet(IsLiteral node)
		throws UnsupportedRdbmsOperatorException
	{
		result = isNotNull(sql.createLabelExpr(node.getArg()));
	}

	@Override
	public void meet(IsResource node)
		throws UnsupportedRdbmsOperatorException
	{
		SqlExpr isBNode = isNotNull(sql.createBNodeExpr(node.getArg()));
		result = or(isBNode, isNotNull(sql.createUriExpr(node.getArg())));
	}

	@Override
	public void meet(IsURI node)
		throws UnsupportedRdbmsOperatorException
	{
		result = isNotNull(sql.createUriExpr(node.getArg()));
	}

	@Override
	public void meet(LangMatches node)
		throws UnsupportedRdbmsOperatorException
	{
		ValueExpr left = node.getLeftArg();
		ValueExpr right = node.getRightArg();
		SqlCase sqlCase = new SqlCase();
		sqlCase.when(eq(label(right), str("*")), neq(label(left), str("")));
		SqlExpr pattern = concat(lowercase(label(right)), str("%"));
		sqlCase.when(new TrueValue(), like(label(left), pattern));
		result = sqlCase;
	}

	@Override
	public void meet(Not node)
		throws UnsupportedRdbmsOperatorException
	{
		result = not(bool(node.getArg()));
	}

	@Override
	public void meet(Or node)
		throws UnsupportedRdbmsOperatorException
	{
		result = or(bool(node.getLeftArg()), bool(node.getRightArg()));
	}

	@Override
	public void meet(Regex node)
		throws UnsupportedRdbmsOperatorException
	{
		result = regex(label(node.getArg()), label(node.getPatternArg()), label(node.getFlagsArg()));
	}

	@Override
	public void meet(SameTerm node)
		throws UnsupportedRdbmsOperatorException
	{
		ValueExpr left = node.getLeftArg();
		ValueExpr right = node.getRightArg();
		boolean leftIsVar = left instanceof Var;
		boolean rightIsVar = right instanceof Var;
		boolean leftIsConst = left instanceof ValueConstant;
		boolean rightIsConst = right instanceof ValueConstant;
		if (leftIsVar && rightIsVar) {
			result = eq(new RefIdColumn((Var)left), new RefIdColumn((Var)right));
		}
		else if ((leftIsVar || leftIsConst) && (rightIsVar || rightIsConst)) {
			result = eq(hash(left), hash(right));
		}
		else {
			SqlExpr bnodes = eqComparingNull(bNode(left), bNode(right));
			SqlExpr uris = eqComparingNull(uri(left), uri(right));
			SqlExpr langs = eqComparingNull(lang(left), lang(right));
			SqlExpr datatype = eqComparingNull(type(left), type(right));
			SqlExpr labels = eqComparingNull(label(left), label(right));

			SqlExpr literals = and(langs, and(datatype, labels));
			result = and(bnodes, and(uris, literals));
		}
	}

	@Override
	public void meet(ValueConstant vc)
		throws UnsupportedRdbmsOperatorException
	{
		result = valueOf(vc.getValue());
	}

	@Override
	public void meet(Var var)
		throws UnsupportedRdbmsOperatorException
	{
		if (var.getValue() == null) {
			result = effectiveBooleanValue(var);
		}
		else {
			result = valueOf(var.getValue());
		}
	}

	public void setSqlExprFactory(SqlExprFactory sql) {
		this.sql = sql;
	}

	protected SqlExpr bNode(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createBNodeExpr(arg);
	}

	protected SqlExpr bool(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createBooleanExpr(arg);
	}

	protected SqlExpr label(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createLabelExpr(arg);
	}

	protected SqlExpr lang(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createLanguageExpr(arg);
	}

	protected SqlExpr hash(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createHashExpr(arg);
	}

	@Override
	protected void meetNode(QueryModelNode arg)
		throws UnsupportedRdbmsOperatorException
	{
		if (arg instanceof ValueExpr) {
			result = effectiveBooleanValue((ValueExpr)arg);
		}
		else {
			throw unsupported(arg);
		}
	}

	protected SqlExpr numeric(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createNumericExpr(arg);
	}

	protected SqlExpr time(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createTimeExpr(arg);
	}

	protected SqlExpr type(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createDatatypeExpr(arg);
	}

	protected SqlExpr uri(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createUriExpr(arg);
	}

	protected SqlExpr zoned(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createZonedExpr(arg);
	}

	private SqlExpr effectiveBooleanValue(ValueExpr v)
		throws UnsupportedRdbmsOperatorException
	{
		String bool = XMLSchema.BOOLEAN.stringValue();
		SqlCase sqlCase = new SqlCase();
		sqlCase.when(eq(type(v), str(bool)), eq(label(v), str("true")));
		sqlCase.when(simple(type(v)), not(eq(label(v), str(""))));
		sqlCase.when(isNotNull(numeric(v)), not(eq(numeric(v), num(0))));
		return sqlCase;
	}

	private SqlExpr equal(ValueExpr left, ValueExpr right)
		throws UnsupportedRdbmsOperatorException
	{
		SqlExpr bnodes = eq(bNode(left), bNode(right));
		SqlExpr uris = eq(uri(left), uri(right));
		SqlCase scase = new SqlCase();
		scase.when(or(isNotNull(bNode(left)), isNotNull(bNode(right))), bnodes);
		scase.when(or(isNotNull(uri(left)), isNotNull(uri(right))), uris);
		return literalEqual(left, right, scase);
	}

	private boolean isTerm(ValueExpr node) {
		return node instanceof Var || node instanceof ValueConstant;
	}

	private SqlExpr literalEqual(ValueExpr left, ValueExpr right, SqlCase scase)
		throws UnsupportedRdbmsOperatorException
	{
		// TODO What about xsd:booleans?
		SqlExpr labels = eq(label(left), label(right));
		SqlExpr langs = and(eqIfNotNull(lang(left), lang(right)), labels.clone());
		SqlExpr numeric = eq(numeric(left), numeric(right));
		SqlExpr time = eq(time(left), time(right));

		SqlExpr bothCalendar = and(isNotNull(time(left)), isNotNull(time(right)));
		SqlExpr over14 = gt(abs(sub(time(left), time(right))), num(HR14 / 2));
		SqlExpr comparable = and(bothCalendar, or(eq(zoned(left), zoned(right)), over14));

		scase.when(or(isNotNull(lang(left)), isNotNull(lang(right))), langs);
		scase.when(and(simple(type(left)), simple(type(right))), labels.clone());
		scase.when(and(isNotNull(numeric(left)), isNotNull(numeric(right))), numeric);
		scase.when(comparable, time);
		scase.when(and(eq(type(left), type(right)), labels.clone()), new TrueValue());
		return scase;
	}

	private SqlExpr termsEqual(ValueExpr left, ValueExpr right)
		throws UnsupportedRdbmsOperatorException
	{
		SqlExpr bnodes = eqIfNotNull(bNode(left), bNode(right));
		SqlExpr uris = eqIfNotNull(uri(left), uri(right));
		SqlCase scase = new SqlCase();
		scase.when(or(isNotNull(bNode(left)), isNotNull(bNode(right))), bnodes);
		scase.when(or(isNotNull(uri(left)), isNotNull(uri(right))), uris);
		return literalEqual(left, right, scase);
	}

	private SqlExpr valueOf(Value value) {
		if (value instanceof Literal) {
			if (((Literal)value).booleanValue()) {
				return new TrueValue();
			}
			return new FalseValue();
		}
		return sqlNull();
	}
}
