/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExprRenderer;
import org.eclipse.rdf4j.federated.algebra.FedXStatementPattern;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.evaluation.SparqlFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.iterator.BoundJoinVALUESConversionIteration;
import org.eclipse.rdf4j.federated.exception.IllegalQueryException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various static functions for query handling and parsing.
 *
 * @author Andreas Schwarte
 */
public class QueryStringUtil {

	/* TODO make operations more performant, i.e. use same instance of StringBuilder more permanently */

	private static final Logger log = LoggerFactory.getLogger(QueryStringUtil.class);

	/**
	 * A dummy URI which is used as a replacement for {@link BNode}s in {@link #appendBNode(StringBuilder, BNode)} since
	 * BNodes cannot be expressed in SPARQL queries
	 */
	public static final IRI BNODE_URI = FedXUtil.iri("http://fluidops.com/fedx/bnode");

	/**
	 * returns true iff there is at least one free variable, i.e. there is no binding for any variable
	 *
	 * @param stmt
	 * @param bindings
	 * @return whether free vars are available
	 */
	public static boolean hasFreeVars(StatementPattern stmt, BindingSet bindings) {
		for (Var var : stmt.getVarList()) {
			if (!var.hasValue() && !bindings.hasBinding(var.getName())) {
				return true; // there is at least one free var
			}
		}
		return false;
	}

	/**
	 * Return a string representation of this statement using the following pattern, where variables are indicated using
	 * ?var and values are represented as strings.
	 *
	 * Pattern: {s; p; o}
	 *
	 * @param stmt
	 * @return a string representation of the statement
	 */
	public static String toString(StatementPattern stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		appendVar(sb, stmt.getSubjectVar(), new HashSet<>(), EmptyBindingSet.getInstance());
		sb.append("; ");
		appendVar(sb, stmt.getPredicateVar(), new HashSet<>(), EmptyBindingSet.getInstance());
		sb.append("; ");
		appendVar(sb, stmt.getObjectVar(), new HashSet<>(), EmptyBindingSet.getInstance());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Converts an {@link ArbitraryLengthPath} node to a sub query string and makes sure to insert any bindings.
	 *
	 * <p>
	 * This method assumes that the {@link ArbitraryLengthPath#getPathExpression()} is a {@link StatementPattern}.
	 * </p>
	 *
	 * @param node
	 * @param varNames
	 * @param bindings
	 * @return the query string
	 */
	public static String toString(ArbitraryLengthPath node, Set<String> varNames, BindingSet bindings) {

		// assumes that the path expr is a statement pattern
		if (!(node.getPathExpression() instanceof StatementPattern)) {
			throw new IllegalArgumentException("Can only handle path expressions of type StatementPattern, was "
					+ node.getPathExpression().getClass());
		}

		StatementPattern stmt = (StatementPattern) node.getPathExpression();

		StringBuilder sb = new StringBuilder();

		sb = appendVar(sb, stmt.getSubjectVar(), varNames, bindings).append(" ");

		// append the path expression with the modifier
		sb = appendVar(sb, stmt.getPredicateVar(), varNames, bindings);
		sb.append(node.getMinLength() == 0 ? "*" : "+");
		sb.append(" ");

		sb = appendVar(sb, stmt.getObjectVar(), varNames, bindings).append(" . ");

		return sb.toString();
	}

	public static String toString(Var var) {
		if (!var.hasValue()) {
			return "?" + var.getName();
		}
		return getValueString(var.getValue());
	}

	/**
	 * Return a string representation of this statement using the following pattern, where variables are indicated using
	 * ?var and values are represented as strings.
	 *
	 * Pattern: {s; p; o}
	 *
	 * @param subj the subject
	 * @param pred the predicate
	 * @param obj  the object
	 * @return a string representation
	 */
	public static String toString(Resource subj, IRI pred, Value obj) {
		return toString(QueryAlgebraUtil.toStatementPattern(subj, pred, obj));
	}

	/**
	 * Construct a SELECT query for the provided statement.
	 *
	 * @param stmt
	 * @param bindings
	 * @param filterExpr
	 * @param evaluated  parameter can be used outside this method to check whether FILTER has been evaluated, false in
	 *                   beginning
	 *
	 * @return the SELECT query
	 * @throws IllegalQueryException if the query does not have any free variables
	 */
	public static String selectQueryString(StatementPattern stmt, BindingSet bindings, FilterValueExpr filterExpr,
			AtomicBoolean evaluated, Dataset dataset) throws IllegalQueryException {

		Set<String> varNames = new HashSet<>();
		String s = constructStatement(stmt, varNames, bindings);

		StringBuilder res = new StringBuilder();

		res.append("SELECT ");

		if (varNames.isEmpty()) {
			throw new IllegalQueryException("SELECT query needs at least one projection!");
		}

		for (String var : varNames) {
			res.append(" ?").append(var);
		}

		res.append(" ");
		appendDatasetClause(res, dataset);
		res.append("WHERE { ").append(s);

		if (filterExpr != null) {
			try {
				String filter = FilterUtils.toSparqlString(filterExpr);
				res.append("FILTER ").append(filter);
				evaluated.set(true);
			} catch (Exception e) {
				log.debug("Filter could not be evaluated remotely. " + e.getMessage());
				log.trace("Details: ", e);
			}
		}

		res.append(" }");

		if (stmt instanceof FedXStatementPattern) {
			long upperLimit = ((FedXStatementPattern) stmt).getUpperLimit();
			if (upperLimit > 0) {
				res.append(" LIMIT ").append(upperLimit);
			}
		}

		return res.toString();
	}

	/**
	 * Construct a SELECT query for the provided {@link ExclusiveTupleExprRenderer}
	 *
	 * @param stmt
	 * @param bindings
	 * @param filterExpr
	 * @param evaluated  parameter can be used outside this method to check whether FILTER has been evaluated, false in
	 *                   beginning
	 *
	 * @return the SELECT query
	 * @throws IllegalQueryException if the query does not have any free variables
	 */
	public static String selectQueryString(ExclusiveTupleExprRenderer expr, BindingSet bindings,
			FilterValueExpr filterExpr,
			AtomicBoolean evaluated, Dataset dataset) throws IllegalQueryException {

		Set<String> varNames = new HashSet<>();
		String s = constructJoinArg(expr, varNames, bindings);

		StringBuilder res = new StringBuilder();

		res.append("SELECT ");

		if (varNames.isEmpty()) {
			throw new IllegalQueryException("SELECT query needs at least one projection!");
		}

		for (String var : varNames) {
			res.append(" ?").append(var);
		}

		res.append(" ");
		appendDatasetClause(res, dataset);
		res.append("WHERE { ").append(s);

		if (filterExpr != null) {
			try {
				String filter = FilterUtils.toSparqlString(filterExpr);
				res.append("FILTER ").append(filter);
				evaluated.set(true);
			} catch (Exception e) {
				log.debug("Filter could not be evaluated remotely. " + e.getMessage());
				log.trace("Details: ", e);
			}
		}

		res.append(" }");

		// TODO add support for this in ExclusiveTupleExprRenderer
//		long upperLimit = stmt.getUpperLimit();
//		if (upperLimit > 0) {
//			res.append(" LIMIT ").append(upperLimit);
//		}
		return res.toString();
	}

	/**
	 * Construct a SELECT query for the provided {@link ExclusiveGroup}. Note that bindings and filterExpr are applied
	 * whenever possible.
	 *
	 * @param group      the expression for the query
	 * @param bindings   the bindings to be applied
	 * @param filterExpr a filter expression or null
	 * @param evaluated  parameter can be used outside this method to check whether FILTER has been evaluated, false in
	 *                   beginning
	 *
	 * @return the SELECT query string
	 * @throws IllegalQueryException
	 *
	 */
	public static String selectQueryString(ExclusiveGroup group, BindingSet bindings, FilterValueExpr filterExpr,
			AtomicBoolean evaluated, Dataset dataset) throws IllegalQueryException {

		StringBuilder sb = new StringBuilder();
		Set<String> varNames = new HashSet<>();

		for (ExclusiveTupleExpr s : group.getExclusiveExpressions()) {
			sb.append(constructJoinArg(s, varNames, bindings));
		}

		if (varNames.isEmpty()) {
			throw new IllegalQueryException("SELECT query needs at least one projection!");
		}

		StringBuilder res = new StringBuilder();
		res.append("SELECT  ");

		for (String var : varNames) {
			res.append(" ?").append(var);
		}

		res.append(" ");
		appendDatasetClause(res, dataset);
		res.append("WHERE { ").append(sb);

		if (filterExpr != null) {
			try {
				String filter = FilterUtils.toSparqlString(filterExpr);
				res.append("FILTER ").append(filter);
				evaluated.set(true);
			} catch (Exception e) {
				log.debug("Filter could not be evaluated remotely. " + e.getMessage());
				log.trace("Details", e);
			}
		}

		res.append(" }");

		return res.toString();
	}

	/**
	 * Transform the {@link ExclusiveTupleExpr} into a ASK query string
	 *
	 * @param expr
	 * @param bindings
	 * @return the ASK query string
	 * @throws IllegalQueryException
	 */
	public static String askQueryString(ExclusiveTupleExpr expr, BindingSet bindings, Dataset dataset) {

		Set<String> varNames = new HashSet<>();

		StringBuilder res = new StringBuilder();
		res.append("ASK ");
		appendDatasetClause(res, dataset);
		res.append("{ ").append(constructJoinArg(expr, varNames, bindings)).append(" }");
		return res.toString();
	}

	/**
	 * Construct a SELECT query string for a bound union.
	 *
	 * Pattern:
	 *
	 * SELECT ?v_1 ?v_2 ?v_N WHERE { { ?v_1 p o } UNION { ?v_2 p o } UNION ... }
	 *
	 * Note that the filterExpr is not evaluated at the moment.
	 *
	 * @param stmt
	 * @param unionBindings
	 * @param filterExpr
	 * @param evaluated     parameter can be used outside this method to check whether FILTER has been evaluated, false
	 *                      in beginning
	 *
	 * @return the SELECT query string
	 * @deprecated replaced with
	 *             {@link #selectQueryStringBoundJoinVALUES(StatementPattern, List, FilterValueExpr, AtomicBoolean)}
	 */
	@Deprecated
	public static String selectQueryStringBoundUnion(StatementPattern stmt, List<BindingSet> unionBindings,
			FilterValueExpr filterExpr, Boolean evaluated, Dataset dataset) {

		Set<String> varNames = new HashSet<>();
		StringBuilder unions = new StringBuilder();
		for (int i = 0; i < unionBindings.size(); i++) {
			String s = constructStatementId(stmt, Integer.toString(i), varNames, unionBindings.get(i));
			if (i > 0) {
				unions.append(" UNION");
			}
			unions.append(" { ").append(s).append(" }");
		}

		StringBuilder res = new StringBuilder();

		res.append("SELECT ");

		for (String var : varNames) {
			res.append(" ?").append(var);
		}

		res.append(" ");
		appendDatasetClause(res, dataset);
		res.append("WHERE { ");

		res.append(unions);

		// TODO evaluate filter expression remote
//		if (filterExpr!=null) {
//
//		}

		res.append(" }");

		return res.toString();
	}

	/**
	 * Creates a bound join subquery using the SPARQL 1.1 VALUES operator.
	 * <p>
	 * Example subquery:
	 * </p>
	 *
	 * <pre>
	 * SELECT ?v ?__index WHERE {
	 *    VALUES (?s ?__index) {
	 *      (:s1 1) (:s2 2)
	 *      ...
	 *      (:sN N)
	 *    }
	 *    ?s name ?v.
	 * }
	 * </pre>
	 *
	 * @param stmt
	 * @param unionBindings
	 * @param filterExpr
	 * @param evaluated     parameter can be used outside this method to check whether FILTER has been evaluated, false
	 *                      in beginning
	 *
	 * @return the SELECT query string
	 * @see SparqlFederationEvalStrategy
	 * @see BoundJoinVALUESConversionIteration
	 * @since 3.0
	 */
	public static String selectQueryStringBoundJoinVALUES(StatementPattern stmt, List<BindingSet> unionBindings,
			FilterValueExpr filterExpr, AtomicBoolean evaluated, Dataset dataset) {

		Set<String> varNames = new LinkedHashSet<>();
		StringBuilder res = new StringBuilder();

		String stmtPattern = constructStatement(stmt, varNames, new EmptyBindingSet());
		res.append("SELECT ");

		for (String var : varNames) {
			res.append(" ?").append(var);
		}

		res.append(" ?").append(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME);

		res.append(" ");
		appendDatasetClause(res, dataset);
		res.append("WHERE {");

		// TODO evaluate filter expression remote
//		if (filterExpr!=null) {
//
//		}

		// add VALUES clause
		res.append(" VALUES (");

		// find relevant bindings
		for (String var : varNames) {
			res.append("?").append(var).append(" ");
		}
		res.append(" ?__index) { ");

		int index = 0;
		for (BindingSet b : unionBindings) {
			res.append("(");
			for (String var : varNames) {
				if (b.hasBinding(var)) {
					appendValue(res, b.getValue(var)).append(" ");
				} else {
					res.append("UNDEF ");
				}
			}
			res.append("\"").append(index).append("\") ");
			index++;
		}

		res.append(" } ");

		res.append(stmtPattern);

		res.append(" }");

		return res.toString();
	}

	/**
	 * Construct a SELECT query for a grouped bound check.
	 *
	 * Pattern:
	 *
	 * SELECT ?o_1 .. ?o_N WHERE { { s1 p1 ?o_1 FILTER ?o_1=o1 } UNION ... UNION { sN pN ?o_N FILTER ?o_N=oN }}
	 *
	 * @param stmt
	 * @param unionBindings
	 * @return the SELECT query string
	 */
	public static String selectQueryStringBoundCheck(StatementPattern stmt, List<BindingSet> unionBindings,
			Dataset dataset) {

		Set<String> varNames = new HashSet<>();

		StringBuilder unions = new StringBuilder();
		for (int i = 0; i < unionBindings.size(); i++) {
			String s = constructStatementCheckId(stmt, i, varNames, unionBindings.get(i));
			if (i > 0) {
				unions.append(" UNION");
			}
			unions.append(" { ").append(s).append(" }");
		}

		StringBuilder res = new StringBuilder();

		res.append("SELECT ");

		for (String var : varNames) {
			res.append(" ?").append(var);
		}

		res.append(" ");
		appendDatasetClause(res, dataset);
		res.append("WHERE {");

		res.append(unions).append(" }");

		return res.toString();
	}

	protected static String constructInnerUnion(StatementPattern stmt, int outerID, Set<String> varNames,
			List<BindingSet> bindings) {

		StringBuilder innerUnion = new StringBuilder();

		for (int idx = 0; idx < bindings.size(); idx++) {
			if (idx > 0) {
				innerUnion.append("UNION ");
			}
			innerUnion.append("{")
					.append(constructStatementId(stmt, outerID + "_" + idx, varNames, bindings.get(idx)))
					.append("} ");
		}

		return innerUnion.toString();
	}

	/**
	 * Construct a query substring from the {@link ExclusiveTupleExpr} that can be used as an argument to a
	 * {@link Join}.
	 * <p>
	 * This method can only be used for {@link ExclusiveTupleExpr} that additionally provide
	 * {@link ExclusiveTupleExprRenderer} capabilities. An exception to this is if the given expression is a
	 * {@link StatementPattern}, e.g. an {@link ExclusiveStatement} or {@link ExclusiveGroup}.
	 * </p>
	 *
	 * @param exclusiveExpr
	 * @param varNames
	 * @param bindings
	 * @return the query string with bindings inserted
	 */
	protected static String constructJoinArg(ExclusiveTupleExpr exclusiveExpr, Set<String> varNames,
			BindingSet bindings) {

		if (exclusiveExpr instanceof StatementPattern) {
			return constructStatement((StatementPattern) exclusiveExpr, varNames, bindings);
		}

		if (exclusiveExpr instanceof ExclusiveGroup) {
			StringBuilder sb = new StringBuilder();
			for (ExclusiveTupleExpr s : ((ExclusiveGroup) exclusiveExpr).getExclusiveExpressions()) {
				sb.append(constructJoinArg(s, varNames, bindings));
			}
			return sb.toString();
		}

		if (!(exclusiveExpr instanceof ExclusiveTupleExprRenderer)) {
			throw new IllegalStateException("Cannot render tupl expr of type " + exclusiveExpr.getClass());
		}

		return ((ExclusiveTupleExprRenderer) exclusiveExpr).toQueryString(varNames, bindings);
	}

	/**
	 * Construct a boolean ASK query for the provided statement.
	 *
	 * @param stmt
	 * @param bindings
	 * @return the ASK query string
	 */
	public static String askQueryString(StatementPattern stmt, BindingSet bindings, Dataset dataset) {

		Set<String> varNames = new HashSet<>();
		String s = constructStatement(stmt, varNames, bindings);

		StringBuilder res = new StringBuilder();

		res.append("ASK ");
		appendDatasetClause(res, dataset);
		res.append(" { ");
		res.append(s).append(" }");

		return res.toString();
	}

	/**
	 * Construct a SELECT query for the provided statement with LIMIT 1. Such query can be used for source selection
	 * instead of ASK queries.
	 *
	 * @param stmt
	 * @param bindings
	 * @return the SELECT query string
	 */
	public static String selectQueryStringLimit1(StatementPattern stmt, BindingSet bindings, Dataset dataset) {

		Set<String> varNames = new HashSet<>();
		String s = constructStatement(stmt, varNames, bindings);

		StringBuilder res = new StringBuilder();

		res.append("SELECT * ");
		appendDatasetClause(res, dataset);
		res.append("WHERE { ");
		res.append(s).append(" } LIMIT 1");

		return res.toString();
	}

	/**
	 * Construct a SELECT query for the provided expr with LIMIT 1. Such query can be used for source selection instead
	 * of ASK queries.
	 *
	 * @param stmt
	 * @param bindings
	 * @return the SELECT query string
	 */
	public static String selectQueryStringLimit1(ExclusiveTupleExpr expr, BindingSet bindings, Dataset dataset) {

		if (expr instanceof ExclusiveGroup) {
			return selectQueryStringLimit1((ExclusiveGroup) expr, bindings, dataset);
		}

		Set<String> varNames = new HashSet<>();
		String s = constructJoinArg(expr, varNames, bindings);

		StringBuilder res = new StringBuilder();

		res.append("SELECT * ");
		appendDatasetClause(res, dataset);
		res.append("WHERE {");
		res.append(s).append(" } LIMIT 1");

		return res.toString();
	}

	/**
	 * Construct a SELECT query for the provided {@link ExclusiveGroup} with LIMIT 1. Such query can be used for source
	 * selection instead of ASK queries.
	 *
	 * @param group
	 * @param bindings
	 * @return the SELECT query string
	 */
	public static String selectQueryStringLimit1(ExclusiveGroup group, BindingSet bindings, Dataset dataset) {

		Set<String> varNames = new HashSet<>();
		StringBuilder res = new StringBuilder();

		res.append("SELECT * ");
		appendDatasetClause(res, dataset);
		res.append("WHERE {");

		for (ExclusiveTupleExpr s : group.getExclusiveExpressions()) {
			res.append(constructJoinArg(s, varNames, bindings));
		}

		res.append(" } LIMIT 1");

		return res.toString();
	}

	/**
	 * Construct the statement string, i.e. "s p o . " with bindings inserted wherever possible. Note that the relevant
	 * free variables are added to the varNames set for further evaluation.
	 *
	 * @param stmt
	 * @param varNames
	 * @param bindings
	 *
	 * @return the constructed statement pattern
	 */
	protected static String constructStatement(StatementPattern stmt, Set<String> varNames, BindingSet bindings) {
		StringBuilder sb = new StringBuilder();

		if (stmt.getScope().equals(Scope.NAMED_CONTEXTS)) {
			sb.append("GRAPH ");
			appendVar(sb, stmt.getContextVar(), varNames, bindings);
			sb.append(" { ");
		}
		sb = appendVar(sb, stmt.getSubjectVar(), varNames, bindings).append(" ");
		sb = appendVar(sb, stmt.getPredicateVar(), varNames, bindings).append(" ");
		sb = appendVar(sb, stmt.getObjectVar(), varNames, bindings).append(" . ");

		if (stmt.getScope().equals(Scope.NAMED_CONTEXTS)) {
			sb.append("} ");
		}

		return sb.toString();
	}

	/**
	 * Construct the statement string, i.e. "s p o . " with bindings inserted wherever possible. Variables are renamed
	 * to "var_"+varId to identify query results in bound queries. Note that the free variables are also added to the
	 * varNames set for further evaluation.
	 *
	 * @param stmt
	 * @param varNames
	 * @param bindings
	 *
	 * @return the constructed statement pattern
	 */
	protected static String constructStatementId(StatementPattern stmt, String varID, Set<String> varNames,
			BindingSet bindings) {
		StringBuilder sb = new StringBuilder();

		sb = appendVarId(sb, stmt.getSubjectVar(), varID, varNames, bindings).append(" ");
		sb = appendVarId(sb, stmt.getPredicateVar(), varID, varNames, bindings).append(" ");
		sb = appendVarId(sb, stmt.getObjectVar(), varID, varNames, bindings).append(" . ");

		return sb.toString();
	}

	/**
	 * Construct the statement string, i.e. "s p ?o_varID FILTER ?o_N=o ". This kind of statement pattern is necessary
	 * to later on identify available results.
	 *
	 * @param stmt
	 * @param varID
	 * @param varNames
	 * @param bindings
	 * @return the statement pattern string
	 */
	protected static String constructStatementCheckId(StatementPattern stmt, int varID, Set<String> varNames,
			BindingSet bindings) {
		StringBuilder sb = new StringBuilder();

		String _varID = Integer.toString(varID);
		sb = appendVarId(sb, stmt.getSubjectVar(), _varID, varNames, bindings).append(" ");
		sb = appendVarId(sb, stmt.getPredicateVar(), _varID, varNames, bindings).append(" ");

		sb.append("?o_").append(_varID);
		varNames.add("o_" + _varID);

		String objValue;
		if (stmt.getObjectVar().hasValue()) {
			objValue = getValueString(stmt.getObjectVar().getValue());
		} else if (bindings.hasBinding(stmt.getObjectVar().getName())) {
			objValue = getValueString(bindings.getBinding(stmt.getObjectVar().getName()).getValue());
		} else {
			// just to make sure that we see an error, will be deleted soon
			throw new RuntimeException("Unexpected.");
		}

		sb.append(" FILTER (?o_").append(_varID).append(" = ").append(objValue).append(" )");

		return sb.toString();
	}

	/**
	 * Append the variable to the provided StringBuilder.
	 *
	 * Cases: 1) unbound: check provided bindingset for possible match a) match found: append matching value b) no
	 * match: append ?varName and add to varNames 2) bound: append value
	 *
	 * @param sb
	 * @param var
	 * @param varNames
	 * @param bindings
	 *
	 * @return the stringbuilder
	 */
	protected static StringBuilder appendVar(StringBuilder sb, Var var, Set<String> varNames, BindingSet bindings) {
		if (!var.hasValue()) {
			if (bindings.hasBinding(var.getName())) {
				return appendValue(sb, bindings.getValue(var.getName()));
			}
			varNames.add(var.getName());
			return sb.append("?").append(var.getName());
		} else {
			return appendValue(sb, var.getValue());
		}
	}

	/**
	 * Append the variable to the provided StringBuilder, however change name of variable by appending "_varId" to it.
	 *
	 * Cases: 1) unbound: check provided bindingset for possible match a) match found: append matching value b) no
	 * match: append ?varName_varId and add to varNames 2) bound: append value
	 *
	 * @param sb
	 * @param var
	 * @param varNames
	 * @param bindings
	 *
	 * @return the complemented string builder
	 */
	protected static StringBuilder appendVarId(StringBuilder sb, Var var, String varID, Set<String> varNames,
			BindingSet bindings) {
		if (!var.hasValue()) {
			if (bindings.hasBinding(var.getName())) {
				return appendValue(sb, bindings.getValue(var.getName()));
			}
			String newName = var.getName() + "_" + varID;
			varNames.add(newName);
			return sb.append("?").append(newName);
		} else {
			return appendValue(sb, var.getValue());
		}
	}

	/**
	 * Return the string representation of this value, see {@link #appendValue(StringBuilder, Value)} for details.
	 *
	 * @param value
	 *
	 * @return the string representation
	 */
	protected static String getValueString(Value value) {
		StringBuilder sb = new StringBuilder();
		appendValue(sb, value);
		return sb.toString();
	}

	/**
	 * Append a string representation of the value to the string builder.
	 *
	 * 1. URI: <http://myUri> 2. Literal: "myLiteral"^^<dataType>
	 *
	 * @param sb
	 * @param value
	 * @return the string builder
	 */
	protected static StringBuilder appendValue(StringBuilder sb, Value value) {

		if (value instanceof IRI) {
			return appendURI(sb, (IRI) value);
		}
		if (value instanceof Literal) {
			return appendLiteral(sb, (Literal) value);
		}
		if (value instanceof BNode) {
			return appendBNode(sb, (BNode) value);
		}
		throw new RuntimeException("Type not supported: " + value.getClass().getCanonicalName());
	}

	/**
	 * Append the uri to the stringbuilder, i.e. <uri.stringValue>.
	 *
	 * @param sb
	 * @param uri
	 * @return the string builder
	 */
	protected static StringBuilder appendURI(StringBuilder sb, IRI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}

	/**
	 * Append a dummy string (see {@link #BNODE_URI}) to represent the BNode.
	 *
	 * Note: currently it is not possible to retrieve values for a BNode via SPARQL, hence we use a dummy BNode which
	 * does not produce any results. A warning is printed to debug.
	 *
	 * @param sb
	 * @param bNode
	 * @return the string builder
	 */
	protected static StringBuilder appendBNode(StringBuilder sb, BNode bNode) {
		log.debug("Cannot express BNodes in SPARQl: Bnode " + bNode.toString() + " is replaced with "
				+ BNODE_URI.stringValue());
		// TODO think how this can be done in queries, for now we just append a
		// dummy URI which does not produce any results
		return appendURI(sb, BNODE_URI);
	}

	/**
	 * Append the literal to the stringbuilder.
	 *
	 * @param sb
	 * @param lit
	 * @return the string builder
	 */
	protected static StringBuilder appendLiteral(StringBuilder sb, Literal lit) {
		sb.append("'''");
		sb.append(lit.getLabel().replace("\"", "\\\""));
		sb.append("'''");

		if (lit.getLanguage().isPresent()) {
			sb.append('@');
			sb.append(lit.getLanguage().get());
		} else {
			if (lit.getDatatype() != null) {
				sb.append("^^<");
				sb.append(lit.getDatatype().stringValue());
				sb.append('>');
			}
		}
		return sb;
	}

	/**
	 * load the queries from a queries file located at the specified path.
	 *
	 * Expected format: - Queries are SPARQL queries in String format - queries are allowed to span several lines - a
	 * query is interpreted to be finished if an empty line occurs
	 *
	 * Ex:
	 *
	 * QUERY1 ... Q1 cntd
	 *
	 * QUERY2
	 *
	 * @param queryFile
	 * @return a list of queries for the query type
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static List<String> loadQueries(String queryFile) throws FileNotFoundException, IOException {
		ArrayList<String> res = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new FileReader(queryFile))) {
			String tmp;
			String tmpQuery = "";
			while ((tmp = in.readLine()) != null) {
				if (tmp.isEmpty()) {
					if (!tmpQuery.isEmpty()) {
						res.add(tmpQuery);
					}
					tmpQuery = "";
				} else {
					tmpQuery = tmpQuery + tmp;
				}
			}
			if (!tmpQuery.isEmpty()) {
				res.add(tmpQuery);
			}
			return res;
		}

	}

	private static StringBuilder appendDatasetClause(StringBuilder sb, Dataset dataset) {
		if (dataset == null) {
			return sb;
		}
		for (IRI context : dataset.getDefaultGraphs()) {
			sb.append("FROM <").append(context.stringValue()).append("> ");
		}
		for (IRI namedContext : dataset.getNamedGraphs()) {
			sb.append("FROM NAMED <").append(namedContext.stringValue()).append("> ");
		}
		return sb;
	}
}
