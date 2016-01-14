/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.algebra.factories;

import static org.openrdf.model.vocabulary.XMLSchema.DECIMAL;
import static org.openrdf.model.vocabulary.XMLSchema.DOUBLE;
import static org.openrdf.model.vocabulary.XMLSchema.FLOAT;
import static org.openrdf.model.vocabulary.XMLSchema.INTEGER;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.in;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.sqlNull;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.str;
import static org.openrdf.sail.rdbms.algebra.base.SqlExprSupport.unsupported;

import org.openrdf.model.Literal;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.query.algebra.Datatype;
import org.openrdf.query.algebra.Lang;
import org.openrdf.query.algebra.MathExpr;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.Str;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;
import org.openrdf.sail.rdbms.algebra.DatatypeColumn;
import org.openrdf.sail.rdbms.algebra.SqlCase;
import org.openrdf.sail.rdbms.algebra.SqlNull;
import org.openrdf.sail.rdbms.algebra.TrueValue;
import org.openrdf.sail.rdbms.algebra.base.SqlExpr;
import org.openrdf.sail.rdbms.exceptions.UnsupportedRdbmsOperatorException;

/**
 * Creates a datatype SQL expression.
 * 
 * @author James Leigh
 * 
 */
public class DatatypeExprFactory extends AbstractQueryModelVisitor<UnsupportedRdbmsOperatorException> {

	protected SqlExpr result;

	public SqlExpr createDatatypeExpr(ValueExpr expr)
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
	public void meet(Datatype node) {
		result = sqlNull();
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
		boolean divide = node.getOperator().equals(MathExpr.MathOp.DIVIDE);
		ValueExpr left = node.getLeftArg();
		ValueExpr right = node.getRightArg();
		SqlCase sqlCase = new SqlCase();
		sqlCase.when(in(str(DOUBLE), type(left), type(right)), str(DOUBLE));
		sqlCase.when(in(str(FLOAT), type(left), type(right)), str(FLOAT));
		sqlCase.when(in(str(DECIMAL), type(left), type(right)), str(DECIMAL));
		sqlCase.when(new TrueValue(), divide ? str(DECIMAL) : str(INTEGER));
		result = sqlCase;
	}

	@Override
	public void meet(Str node)
		throws UnsupportedRdbmsOperatorException
	{
		result = sqlNull();
	}

	@Override
	public void meet(ValueConstant vc) {
		result = valueOf(vc.getValue());
	}

	@Override
	public void meet(Var var) {
		if (var.getValue() == null) {
			result = new DatatypeColumn(var);
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

	private SqlExpr valueOf(Value value) {
		if (value instanceof Literal) {
			IRI datatype = ((Literal)value).getDatatype();
			if (datatype != null)
				return str(datatype.stringValue());
		}
		return sqlNull();
	}

	private SqlExpr type(ValueExpr expr)
		throws UnsupportedRdbmsOperatorException
	{
		return createDatatypeExpr(expr);
	}

}