/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * Supplies various query model statistics to the query engine/optimizer.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class EvaluationStatistics {

	// static UUID as prefix together with a thread safe incrementing long ensures a unique identifier.
	private final static String uniqueIdPrefix = UUID.randomUUID().toString().replace("-", "");
	private final static AtomicLong uniqueIdSuffix = new AtomicLong();

	private CardinalityCalculator calculator;

	public double getCardinality(TupleExpr expr) {
		if (calculator == null) {
			calculator = createCardinalityCalculator();
			assert calculator != null;
		}

		expr.visit(calculator);
		return calculator.getCardinality();
	}

	protected CardinalityCalculator createCardinalityCalculator() {
		return new CardinalityCalculator();
	}

	/*-----------------------------------*
	 * Inner class CardinalityCalculator *
	 *-----------------------------------*/

	protected static class CardinalityCalculator extends AbstractQueryModelVisitor<RuntimeException> {

		private static final double VAR_CARDINALITY = 10;
		private static final double UNBOUND_SERVICE_CARDINALITY = 100000;

		protected double cardinality;

		public double getCardinality() {
			return cardinality;
		}

		@Override
		public void meet(EmptySet node) {
			// no binding sets
			cardinality = 0.0;
		}

		@Override
		public void meet(SingletonSet node) {
			// one empty binding set
			cardinality = 1.0;
		}

		@Override
		public void meet(BindingSetAssignment node) {
			// actual cardinality is node.getBindingSets().size() binding sets
			// but cost is cheap as we don't need to query the triple store
			// so effective cardinality is 1 or a very slowly increasing function of node.getBindingSets().size().
			cardinality = 1.0;
		}

		@Override
		public void meet(ZeroLengthPath node) {
			Var subjVar = node.getSubjectVar();
			Var objVar = node.getObjectVar();
			if ((subjVar != null && subjVar.hasValue()) || (objVar != null && objVar.hasValue())) {
				// subj = obj
				cardinality = 1.0;
			} else {
				// actual cardinality = count(union(subjs, objs))
				// but cost is equivalent to ?s ?p ?o ?c (impl scans all statements)
				// so due to the lower actual cardinality we value it in preference to a fully unbound statement
				// pattern.
				cardinality = getSubjectCardinality(subjVar) * getObjectCardinality(objVar)
						* getContextCardinality(node.getContextVar());
			}
		}

		@Override
		public void meet(ArbitraryLengthPath node) {
			final Var pathVar = new Var("_anon_" + uniqueIdPrefix + uniqueIdSuffix.incrementAndGet(), true);
			// cardinality of ALP is determined based on the cost of a
			// single ?s ?p ?o ?c pattern where ?p is unbound, compensating for the fact that
			// the length of the path is unknown but expected to be _at least_ twice that of a normal
			// statement pattern.
			cardinality = 2.0 * getCardinality(
					new StatementPattern(node.getSubjectVar().clone(), pathVar, node.getObjectVar().clone(),
							node.getContextVar() != null ? node.getContextVar().clone() : null));
		}

		@Override
		public void meet(Service node) {
			if (!node.getServiceRef().hasValue()) {
				// the URI is not available, may be computed in the course of the
				// query
				// => use high cost to order the SERVICE node late in the query plan
				cardinality = UNBOUND_SERVICE_CARDINALITY;
			} else {
				ServiceNodeAnalyzer serviceAnalyzer = new ServiceNodeAnalyzer();
				node.visitChildren(serviceAnalyzer);
				int count = serviceAnalyzer.getStatementCount();

				// more than one free variable in a single triple pattern
				if (count == 1 && node.getServiceVars().size() > 1) {
					cardinality = 100 + node.getServiceVars().size(); // TODO (should
					// be higher
					// than other
					// simple
					// stmts)
				} else {
					// only very selective statements should be better than this
					// => evaluate service expressions first
					cardinality = 1 + (node.getServiceVars().size() * 0.1);
				}
			}
		}

		@Override
		public void meet(StatementPattern sp) {
			cardinality = getCardinality(sp);
		}

		@Override
		public void meet(TripleRef tripleRef) {
			cardinality = getSubjectCardinality(tripleRef.getSubjectVar())
					* getPredicateCardinality(tripleRef.getPredicateVar())
					* getObjectCardinality(tripleRef.getObjectVar());
		}

		protected double getCardinality(StatementPattern sp) {
			if (!sp.isCardinalitySet()) {
				sp.setCardinality(getSubjectCardinality(sp) * getPredicateCardinality(sp) * getObjectCardinality(sp)
						* getContextCardinality(sp));
			}
			return sp.getCardinality();
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the subjectVar itself but also
		 * the other vars (e.g. the predicate value might determine a subject subset).
		 */
		protected double getSubjectCardinality(StatementPattern sp) {
			return getSubjectCardinality(sp.getSubjectVar());
		}

		protected double getSubjectCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the predicateVar itself but also
		 * the other vars (e.g. the subject value might determine a predicate subset).
		 */
		protected double getPredicateCardinality(StatementPattern sp) {
			return getPredicateCardinality(sp.getPredicateVar());
		}

		protected double getPredicateCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the objectVar itself but also
		 * the other vars (e.g. the predicate value might determine an object subset).
		 */
		protected double getObjectCardinality(StatementPattern sp) {
			return getObjectCardinality(sp.getObjectVar());
		}

		protected double getObjectCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the contextVar itself but also
		 * the other vars (e.g. the subject value might determine a context subset).
		 */
		protected double getContextCardinality(StatementPattern sp) {
			return getContextCardinality(sp.getContextVar());
		}

		protected double getContextCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		protected double getCardinality(double varCardinality, Var var) {
			return (var == null || var.hasValue()) ? 1.0 : varCardinality;
		}

		protected double getCardinality(double varCardinality, Collection<Var> vars) {
			int constantVarCount = countConstantVars(vars);
			double unboundVarFactor = vars.size() - constantVarCount;
			return Math.pow(varCardinality, unboundVarFactor);
		}

		protected int countConstantVars(Iterable<Var> vars) {
			int constantVarCount = 0;

			for (Var var : vars) {
				if (var.hasValue()) {
					constantVarCount++;
				}
			}

			return constantVarCount;
		}

		@Override
		public void meet(Join node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality *= leftArgCost;
		}

		@Override
		public void meet(LeftJoin node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality *= leftArgCost;
		}

		@Override
		protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality += leftArgCost;
		}

		@Override
		protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
			node.getArg().visit(this);
		}

		@Override
		public void meet(QueryRoot node) {
			node.getArg().visit(this);
		}

		@Override
		protected void meetNode(QueryModelNode node) {
			throw new IllegalArgumentException("Unhandled node type: " + node.getClass());
		}
	}

	// count the number of triple patterns
	private static class ServiceNodeAnalyzer extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private int count = 0;

		private ServiceNodeAnalyzer() {
			super(true);
		}

		public int getStatementCount() {
			return count;
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			count++;
		}
	}

}
