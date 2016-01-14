/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.algebra.factories;

import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.and;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.coalesce;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.isNotNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.isNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.sqlNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.str;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.unsupported;

import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.Datatype;
import org.openrdf.query.algebra.Lang;
import org.openrdf.query.algebra.MathExpr;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.Str;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;
import org.openrdf.sail.rdbms.algebra.LongURIColumn;
import org.openrdf.sail.rdbms.algebra.SqlCase;
import org.openrdf.sail.rdbms.algebra.SqlNull;
import org.openrdf.sail.rdbms.algebra.URIColumn;
import org.openrdf.sail.rdbms.algebra.base.SqlExpr;
import org.openrdf.sail.rdbms.exceptions.UnsupportedRdbmsOperatorException;

/**
 * Creates an SQL expression for a URI's string value.
 * 
 * @author James Leigh
 * 
 */
public class URIExprFactory extends AbstractQueryModelVisitor<UnsupportedRdbmsOperatorException> {

	protected SqlExpr result;

	private SqlExprFactory sql;

	public void setSqlExprFactory(SqlExprFactory sql) {
		this.sql = sql;
	}

	public SqlExpr createUriExpr(ValueExpr expr)
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
	public void meet(Datatype node)
		throws UnsupportedRdbmsOperatorException
	{
		SqlCase sqlCase = new SqlCase();
		sqlCase.when(isNotNull(type(node.getArg())), type(node.getArg()));
		sqlCase.when(isNotNull(lang(node.getArg())), str(RDF.LANGSTRING.stringValue()));
		sqlCase.when(and(isNull(lang(node.getArg())), isNotNull(label(node.getArg()))),
				str(XMLSchema.STRING.stringValue()));
		result = sqlCase;
	}

	@Override
	public void meet(Lang node)
		throws UnsupportedRdbmsOperatorException
	{
		result = sqlNull();
	}

	@Override
	public void meet(MathExpr node)
		throws UnsupportedRdbmsOperatorException
	{
		result = sqlNull();
	}

	@Override
	public void meet(Str node) {
		result = sqlNull();
	}

	@Override
	public void meet(ValueConstant vc) {
		result = valueOf(vc.getValue());
	}

	@Override
	public void meet(Var var) {
		if (var.getValue() == null) {
			result = coalesce(new URIColumn(var), new LongURIColumn(var));
		}
		else {
			result = valueOf(var.getValue());
		}
	}

	@Override
	protected void meetNode(QueryModelNode arg)
		throws UnsupportedRdbmsOperatorException
	{
		throw unsupported(arg);
	}

	private SqlExpr label(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createLabelExpr(arg);
	}

	private SqlExpr lang(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createLanguageExpr(arg);
	}

	private SqlExpr type(ValueExpr arg)
		throws UnsupportedRdbmsOperatorException
	{
		return sql.createDatatypeExpr(arg);
	}

	private SqlExpr valueOf(Value value) {
		if (value instanceof IRI) {
			return str(((IRI)value).stringValue());
		}
		return sqlNull();
	}

}
