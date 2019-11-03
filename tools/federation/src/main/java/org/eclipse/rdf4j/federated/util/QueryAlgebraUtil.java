/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.exception.IllegalQueryException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various static functions for query handling and parsing (alegbra expression).
 * 
 * @author Andreas Schwarte
 */
public class QueryAlgebraUtil {

	private static final Logger log = LoggerFactory.getLogger(QueryAlgebraUtil.class);

	/**
	 * returns true iff there is at least one free variable, i.e. there is no binding for any variable
	 * 
	 * @param stmt
	 * @param bindings
	 * @return whether there is at least one free variable
	 */
	public static boolean hasFreeVars(StatementPattern stmt, BindingSet bindings) {
		for (Var var : stmt.getVarList()) {
			if (!var.hasValue() && !bindings.hasBinding(var.getName()))
				return true; // there is at least one free var
		}
		return false;
	}

	/**
	 * Return the {@link Value} of the variable which is either taken from the variable itself (bound) or from the
	 * bindingsset (unbound).
	 * 
	 * @param var
	 * @param bindings the bindings, must not be null, use {@link EmptyBindingSet} instead
	 * 
	 * @return the value or null
	 */
	public static Value getVarValue(Var var, BindingSet bindings) {
		if (var == null) {
			return null;
		} else if (var.hasValue()) {
			return var.getValue();
		} else {
			return bindings.getValue(var.getName());
		}
	}

	public static StatementPattern toStatementPattern(Statement stmt) {
		return toStatementPattern(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
	}

	public static StatementPattern toStatementPattern(Resource subj, IRI pred, Value obj) {
		Var s = subj == null ? new Var("s") : new Var("const_s", subj);
		Var p = pred == null ? new Var("p") : new Var("const_p", pred);
		Var o = obj == null ? new Var("o") : new Var("const_o", obj);
		// TODO context

		return new StatementPattern(s, p, o);
	}

	public static Statement toStatement(StatementPattern stmt) {
		return toStatement(stmt, EmptyBindingSet.getInstance());
	}

	public static Statement toStatement(StatementPattern stmt, BindingSet bindings) {

		Value subj = getVarValue(stmt.getSubjectVar(), bindings);
		Value pred = getVarValue(stmt.getPredicateVar(), bindings);
		Value obj = getVarValue(stmt.getObjectVar(), bindings);
		// TODO context

		return FedXUtil.valueFactory().createStatement((Resource) subj, (IRI) pred, obj);
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
	 * @throws IllegalQueryException
	 */
	public static TupleExpr selectQuery(StatementPattern stmt, BindingSet bindings, FilterValueExpr filterExpr,
			AtomicBoolean evaluated) throws IllegalQueryException {

		Set<String> varNames = new HashSet<>();
		TupleExpr expr = constructStatement(stmt, varNames, bindings);

		if (varNames.isEmpty())
			throw new IllegalQueryException("SELECT query needs at least one projection!");

		if (filterExpr != null) {
			try {
				expr = new Filter(expr, FilterUtils.toFilter(filterExpr));
				evaluated.set(true);
			} catch (Exception e) {
				log.debug("Filter could not be evaluated remotely: " + e.getMessage());
				log.trace("Details: ", e);
			}
		}

		ProjectionElemList projList = new ProjectionElemList();
		for (String var : varNames)
			projList.addElement(new ProjectionElem(var));

		Projection proj = new Projection(expr, projList);

		return proj;
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
	 * @return the SELECT query
	 */
	public static TupleExpr selectQuery(ExclusiveGroup group, BindingSet bindings, FilterValueExpr filterExpr,
			AtomicBoolean evaluated) {

		Set<String> varNames = new HashSet<>();
		List<ExclusiveStatement> stmts = group.getStatements();

		Join join = null;

		if (stmts.size() == 2) {
			join = new Join(constructStatement(stmts.get(0), varNames, bindings),
					constructStatement(stmts.get(1), varNames, bindings));
		} else {
			join = new Join();
			join.setLeftArg(constructStatement(stmts.get(0), varNames, bindings));
			Join tmp = join;
			int idx;
			for (idx = 1; idx < stmts.size() - 1; idx++) {
				Join _u = new Join();
				_u.setLeftArg(constructStatement(stmts.get(idx), varNames, bindings));
				tmp.setRightArg(_u);
				tmp = _u;
			}
			tmp.setRightArg(constructStatement(stmts.get(idx), varNames, bindings));
		}

		TupleExpr expr = join;

		if (filterExpr != null) {
			try {
				expr = new Filter(expr, FilterUtils.toFilter(filterExpr));
				evaluated.set(true);
			} catch (Exception e) {
				log.debug("Filter could not be evaluated remotely: " + e.getMessage());
				log.trace("Details:", e);
			}
		}

		ProjectionElemList projList = new ProjectionElemList();
		for (String var : varNames)
			projList.addElement(new ProjectionElem(var));

		Projection proj = new Projection(expr, projList);

		return proj;
	}

	/**
	 * Construct a SELECT query expression for a bound union.
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
	 * @return the SELECT query
	 */
	public static TupleExpr selectQueryBoundUnion(StatementPattern stmt, List<BindingSet> unionBindings,
			FilterValueExpr filterExpr, Boolean evaluated) {

		// TODO add FILTER expressions

		Set<String> varNames = new HashSet<>();

		Union union = new Union();
		union.setLeftArg(constructStatementId(stmt, Integer.toString(0), varNames, unionBindings.get(0)));
		Union tmp = union;
		int idx;
		for (idx = 1; idx < unionBindings.size() - 1; idx++) {
			Union _u = new Union();
			_u.setLeftArg(constructStatementId(stmt, Integer.toString(idx), varNames, unionBindings.get(idx)));
			tmp.setRightArg(_u);
			tmp = _u;
		}
		tmp.setRightArg(constructStatementId(stmt, Integer.toString(idx), varNames, unionBindings.get(idx)));

		ProjectionElemList projList = new ProjectionElemList();
		for (String var : varNames)
			projList.addElement(new ProjectionElem(var));

		Projection proj = new Projection(union, projList);

		return proj;
	}

	/**
	 * Construct a SELECT query for a grouped bound check.
	 * 
	 * Pattern:
	 * 
	 * SELECT DISTINCT ?o_1 .. ?o_N WHERE { { s1 p1 ?o_1 FILTER ?o_1=o1 } UNION ... UNION { sN pN ?o_N FILTER ?o_N=oN }}
	 * 
	 * @param stmt
	 * @param unionBindings
	 * @return the SELECT query
	 */
	public static TupleExpr selectQueryStringBoundCheck(StatementPattern stmt, List<BindingSet> unionBindings) {

		Set<String> varNames = new HashSet<>();

		Union union = new Union();
		union.setLeftArg(constructStatementCheckId(stmt, 0, varNames, unionBindings.get(0)));
		Union tmp = union;
		int idx;
		for (idx = 1; idx < unionBindings.size() - 1; idx++) {
			Union _u = new Union();
			_u.setLeftArg(constructStatementCheckId(stmt, idx, varNames, unionBindings.get(idx)));
			tmp.setRightArg(_u);
			tmp = _u;
		}
		tmp.setRightArg(constructStatementCheckId(stmt, idx, varNames, unionBindings.get(idx)));

		ProjectionElemList projList = new ProjectionElemList();
		for (String var : varNames)
			projList.addElement(new ProjectionElem(var));

		Projection proj = new Projection(union, projList);

		return proj;
	}

	protected static Union constructInnerUnion(StatementPattern stmt, int outerID, Set<String> varNames,
			List<BindingSet> bindings) {

		Union union = new Union();
		union.setLeftArg(constructStatementId(stmt, outerID + "_0", varNames, bindings.get(0)));
		Union tmp = union;
		int idx;
		for (idx = 1; idx < bindings.size() - 1; idx++) {
			Union _u = new Union();
			_u.setLeftArg(constructStatementId(stmt, outerID + "_" + idx, varNames, bindings.get(idx)));
			tmp.setRightArg(_u);
			tmp = _u;
		}
		tmp.setRightArg(constructStatementId(stmt, outerID + "_" + idx, varNames, bindings.get(idx)));

		return union;
	}

	/**
	 * Construct the statement string, i.e. "s p o . " with bindings inserted wherever possible. Note that the free
	 * variables are added to the varNames set for further evaluation.
	 * 
	 * @param stmt
	 * @param varNames
	 * @param bindings
	 * 
	 * @return the {@link StatementPattern}
	 */
	protected static StatementPattern constructStatement(StatementPattern stmt, Set<String> varNames,
			BindingSet bindings) {

		Var subj = appendVar(stmt.getSubjectVar(), varNames, bindings);
		Var pred = appendVar(stmt.getPredicateVar(), varNames, bindings);
		Var obj = appendVar(stmt.getObjectVar(), varNames, bindings);

		return new StatementPattern(subj, pred, obj);
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
	 * @return the {@link StatementPattern}
	 */
	protected static StatementPattern constructStatementId(StatementPattern stmt, String varID, Set<String> varNames,
			BindingSet bindings) {

		Var subj = appendVarId(stmt.getSubjectVar(), varID, varNames, bindings);
		Var pred = appendVarId(stmt.getPredicateVar(), varID, varNames, bindings);
		Var obj = appendVarId(stmt.getObjectVar(), varID, varNames, bindings);

		return new StatementPattern(subj, pred, obj);
	}

	/**
	 * Construct the statement string, i.e. "s p ?o_varID FILTER ?o_N=o ". This kind of statement pattern is necessary
	 * to later on identify available results.
	 * 
	 * @param stmt
	 * @param varID
	 * @param varNames
	 * @param bindings
	 * @return the expression
	 */
	protected static TupleExpr constructStatementCheckId(StatementPattern stmt, int varID, Set<String> varNames,
			BindingSet bindings) {

		String _varID = Integer.toString(varID);
		Var subj = appendVarId(stmt.getSubjectVar(), _varID, varNames, bindings);
		Var pred = appendVarId(stmt.getPredicateVar(), _varID, varNames, bindings);

		Var obj = new Var("o_" + _varID);
		varNames.add("o_" + _varID);

		Value objValue;
		if (stmt.getObjectVar().hasValue()) {
			objValue = stmt.getObjectVar().getValue();
		} else if (bindings.hasBinding(stmt.getObjectVar().getName())) {
			objValue = bindings.getBinding(stmt.getObjectVar().getName()).getValue();
		} else {
			// just to make sure that we see an error, will be deleted soon
			throw new RuntimeException("Unexpected.");
		}

		Compare cmp = new Compare(obj, new ValueConstant(objValue));
		cmp.setOperator(CompareOp.EQ);
		Filter filter = new Filter(new StatementPattern(subj, pred, obj), cmp);

		return filter;
	}

	/**
	 * Clone the specified variable and attach bindings.
	 * 
	 * @param var
	 * @param varNames
	 * @param bindings
	 * 
	 * @return the variable
	 * 
	 */
	protected static Var appendVar(Var var, Set<String> varNames, BindingSet bindings) {
		Var res = var.clone();
		if (!var.hasValue()) {
			if (bindings.hasBinding(var.getName()))
				res.setValue(bindings.getValue(var.getName()));
			else
				varNames.add(var.getName());
		}
		return res;
	}

	/**
	 * Clone the specified variable and attach bindings, moreover change name of variable by appending "_varId" to it.
	 * 
	 * @param var
	 * @param varID
	 * @param varNames
	 * @param bindings
	 * 
	 * @return the variable
	 */
	protected static Var appendVarId(Var var, String varID, Set<String> varNames, BindingSet bindings) {
		Var res = var.clone();
		if (!var.hasValue()) {
			if (bindings.hasBinding(var.getName())) {
				res.setValue(bindings.getValue(var.getName()));
			} else {
				String newName = var.getName() + "_" + varID;
				varNames.add(newName);
				res.setName(newName);
			}
		}
		return res;
	}

}
