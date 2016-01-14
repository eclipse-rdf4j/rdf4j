/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.algebra.base;

import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;
import org.openrdf.sail.rdbms.algebra.BNodeColumn;
import org.openrdf.sail.rdbms.algebra.DatatypeColumn;
import org.openrdf.sail.rdbms.algebra.DateTimeColumn;
import org.openrdf.sail.rdbms.algebra.DoubleValue;
import org.openrdf.sail.rdbms.algebra.FalseValue;
import org.openrdf.sail.rdbms.algebra.HashColumn;
import org.openrdf.sail.rdbms.algebra.IdColumn;
import org.openrdf.sail.rdbms.algebra.JoinItem;
import org.openrdf.sail.rdbms.algebra.LabelColumn;
import org.openrdf.sail.rdbms.algebra.LanguageColumn;
import org.openrdf.sail.rdbms.algebra.LongLabelColumn;
import org.openrdf.sail.rdbms.algebra.LongURIColumn;
import org.openrdf.sail.rdbms.algebra.NumberValue;
import org.openrdf.sail.rdbms.algebra.NumericColumn;
import org.openrdf.sail.rdbms.algebra.RefIdColumn;
import org.openrdf.sail.rdbms.algebra.SelectProjection;
import org.openrdf.sail.rdbms.algebra.SelectQuery;
import org.openrdf.sail.rdbms.algebra.SqlAbs;
import org.openrdf.sail.rdbms.algebra.SqlAnd;
import org.openrdf.sail.rdbms.algebra.SqlCase;
import org.openrdf.sail.rdbms.algebra.SqlCast;
import org.openrdf.sail.rdbms.algebra.SqlCompare;
import org.openrdf.sail.rdbms.algebra.SqlConcat;
import org.openrdf.sail.rdbms.algebra.SqlEq;
import org.openrdf.sail.rdbms.algebra.SqlIsNull;
import org.openrdf.sail.rdbms.algebra.SqlLike;
import org.openrdf.sail.rdbms.algebra.SqlLowerCase;
import org.openrdf.sail.rdbms.algebra.SqlMathExpr;
import org.openrdf.sail.rdbms.algebra.SqlNot;
import org.openrdf.sail.rdbms.algebra.SqlNull;
import org.openrdf.sail.rdbms.algebra.SqlOr;
import org.openrdf.sail.rdbms.algebra.SqlRegex;
import org.openrdf.sail.rdbms.algebra.SqlShift;
import org.openrdf.sail.rdbms.algebra.StringValue;
import org.openrdf.sail.rdbms.algebra.TrueValue;
import org.openrdf.sail.rdbms.algebra.URIColumn;
import org.openrdf.sail.rdbms.algebra.UnionItem;

/**
 * Base class for RDBMS visitor classes. This class is extended with additional
 * meet methods.
 * 
 * @author James Leigh
 * 
 */
public class RdbmsQueryModelVisitorBase<X extends Exception> extends AbstractQueryModelVisitor<X> {

	public void meet(BNodeColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(DatatypeColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(DateTimeColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(DoubleValue node)
		throws X
	{
		meetSqlConstant(node);
	}

	public void meet(FalseValue node)
		throws X
	{
		meetSqlConstant(node);
	}

	public void meet(HashColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(IdColumn node)
		throws X
	{
		meetSqlExpr(node);
	}

	public void meet(JoinItem node)
		throws X
	{
		meetFromItem(node);
	}

	public void meet(LabelColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(LanguageColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(LongLabelColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(LongURIColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(NumberValue node)
		throws X
	{
		meetSqlConstant(node);
	}

	public void meet(NumericColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(RefIdColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	public void meet(SelectProjection node)
		throws X
	{
		meetNode(node);
	}

	public void meet(SelectQuery node)
		throws X
	{
		meetNode(node);
	}

	public void meet(SqlAbs node)
		throws X
	{
		meetUnarySqlOperator(node);
	}

	public void meet(SqlAnd node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlCase node)
		throws X
	{
		meetNode(node);
	}

	public void meet(SqlCast node)
		throws X
	{
		meetUnarySqlOperator(node);
	}

	public void meet(SqlCompare node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlConcat node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlEq node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlIsNull node)
		throws X
	{
		meetUnarySqlOperator(node);
	}

	public void meet(SqlLike node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlLowerCase node)
		throws X
	{
		meetUnarySqlOperator(node);
	}

	public void meet(SqlMathExpr node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlNot node)
		throws X
	{
		meetUnarySqlOperator(node);
	}

	public void meet(SqlNull node)
		throws X
	{
		meetSqlConstant(node);
	}

	public void meet(SqlOr node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlRegex node)
		throws X
	{
		meetBinarySqlOperator(node);
	}

	public void meet(SqlShift node)
		throws X
	{
		meetUnarySqlOperator(node);
	}

	public void meet(StringValue node)
		throws X
	{
		meetSqlConstant(node);
	}

	public void meet(TrueValue node)
		throws X
	{
		meetSqlConstant(node);
	}

	public void meet(UnionItem node)
		throws X
	{
		meetFromItem(node);
	}

	public void meet(URIColumn node)
		throws X
	{
		meetValueColumnBase(node);
	}

	protected void meetBinarySqlOperator(BinarySqlOperator node)
		throws X
	{
		meetNode(node);
	}

	protected void meetFromItem(FromItem node)
		throws X
	{
		meetNode(node);
	}

	protected void meetSqlConstant(SqlConstant<?> node)
		throws X
	{
		meetNode(node);
	}

	protected void meetSqlExpr(SqlExpr node)
		throws X
	{
		meetNode(node);
	}

	protected void meetUnarySqlOperator(UnarySqlOperator node)
		throws X
	{
		meetNode(node);
	}

	protected void meetValueColumnBase(ValueColumnBase node)
		throws X
	{
		meetSqlExpr(node);
	}
}
