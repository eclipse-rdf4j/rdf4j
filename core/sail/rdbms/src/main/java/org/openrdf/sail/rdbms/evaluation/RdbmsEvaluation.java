/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.openrdf.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.openrdf.sail.rdbms.RdbmsTripleRepository;
import org.openrdf.sail.rdbms.RdbmsValueFactory;
import org.openrdf.sail.rdbms.algebra.ColumnVar;
import org.openrdf.sail.rdbms.algebra.SelectProjection;
import org.openrdf.sail.rdbms.algebra.SelectQuery;
import org.openrdf.sail.rdbms.algebra.SelectQuery.OrderElem;
import org.openrdf.sail.rdbms.exceptions.RdbmsException;
import org.openrdf.sail.rdbms.exceptions.RdbmsQueryEvaluationException;
import org.openrdf.sail.rdbms.exceptions.UnsupportedRdbmsOperatorException;
import org.openrdf.sail.rdbms.iteration.RdbmsBindingIteration;
import org.openrdf.sail.rdbms.schema.IdSequence;

/**
 * Extends the default strategy by accepting {@link SelectQuery} and evaluating
 * them on a database.
 * 
 * @author James Leigh
 * 
 */
public class RdbmsEvaluation extends SimpleEvaluationStrategy {

	private Logger logger = LoggerFactory.getLogger(RdbmsEvaluation.class);

	private QueryBuilderFactory factory;

	private RdbmsValueFactory vf;

	private RdbmsTripleRepository triples;

	private IdSequence ids;

	public RdbmsEvaluation(QueryBuilderFactory factory, RdbmsTripleRepository triples, Dataset dataset,
			IdSequence ids, FederatedServiceResolver serviceManager)
	{
		super(new RdbmsTripleSource(triples), dataset, serviceManager);
		this.factory = factory;
		this.triples = triples;
		this.vf = triples.getValueFactory();
		this.ids = ids;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr,
			BindingSet bindings)
		throws QueryEvaluationException
	{
		if (expr instanceof SelectQuery)
			return evaluate((SelectQuery)expr, bindings);
		return super.evaluate(expr, bindings);
	}

	private CloseableIteration<BindingSet, QueryEvaluationException> evaluate(SelectQuery qb, BindingSet b)
		throws UnsupportedRdbmsOperatorException, RdbmsQueryEvaluationException
	{
		List<Object> parameters = new ArrayList<Object>();
		try {
			QueryBindingSet bindings = new QueryBindingSet(b);
			String query = toQueryString(qb, bindings, parameters);
			try {
				Connection conn = triples.getConnection();
				PreparedStatement stmt = conn.prepareStatement(query);
				int p = 0;
				for (Object o : parameters) {
					stmt.setObject(++p, o);
				}
				Collection<ColumnVar> proj = qb.getProjections();
				RdbmsBindingIteration result = new RdbmsBindingIteration(stmt);
				result.setProjections(proj);
				result.setBindings(bindings);
				result.setValueFactory(vf);
				result.setIdSequence(ids);
				return result;
			}
			catch (SQLException e) {
				throw new RdbmsQueryEvaluationException(e.toString() + "\n" + query, e);
			}
		}
		catch (RdbmsException e) {
			throw new RdbmsQueryEvaluationException(e);
		}
	}

	private String toQueryString(SelectQuery qb, QueryBindingSet bindings, List<Object> parameters)
		throws RdbmsException, UnsupportedRdbmsOperatorException
	{
		QueryBuilder query = factory.createQueryBuilder();
		if (qb.isDistinct()) {
			query.distinct();
		}
		query.from(qb.getFrom());
		for (ColumnVar var : qb.getVars()) {
			for (String name : qb.getBindingNames(var)) {
				if (var.getValue() == null && bindings.hasBinding(name)) {
					query.filter(var, bindings.getValue(name));
				}
				else if (var.getValue() != null && !bindings.hasBinding(name)
						&& qb.getBindingNames().contains(name))
				{
					bindings.addBinding(name, var.getValue());
				}
			}
		}
		int index = 0;
		for (SelectProjection proj : qb.getSqlSelectVar()) {
			ColumnVar var = proj.getVar();
			if (!var.isHiddenOrConstant()) {
				for (String name : qb.getBindingNames(var)) {
					if (!bindings.hasBinding(name)) {
						var.setIndex(index);
						query.select(proj.getId());
						query.select(proj.getStringValue());
						index += 2;
						if (var.getTypes().isLiterals()) {
							query.select(proj.getLanguage());
							query.select(proj.getDatatype());
							index += 2;
						}
					}
				}
			}
		}
		for (OrderElem by : qb.getOrderElems()) {
			query.orderBy(by.sqlExpr, by.isAscending);
			if (qb.isDistinct()) {
				query.select(by.sqlExpr);
			}
		}
		if (qb.getLimit() != null) {
			query.limit(qb.getLimit());
		}
		if (qb.getOffset() != null) {
			query.offset(qb.getOffset());
		}
		parameters.addAll(query.getParameters());
		if (logger.isDebugEnabled()) {
			logger.debug(query.toString());
			logger.debug(parameters.toString());
		}
		return query.toString();
	}
}
