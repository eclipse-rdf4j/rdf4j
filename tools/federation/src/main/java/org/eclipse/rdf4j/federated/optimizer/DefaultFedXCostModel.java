/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.NUnion;
import org.eclipse.rdf4j.federated.algebra.StatementSourcePattern;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link FedXCostModel}
 *
 * @author Andreas Schwarte
 *
 */
public class DefaultFedXCostModel implements FedXCostModel {

	public static DefaultFedXCostModel INSTANCE = new DefaultFedXCostModel();

	private static final Logger log = LoggerFactory.getLogger(DefaultFedXCostModel.class);

	@Override
	public double estimateCost(TupleExpr tupleExpr, Set<String> joinVars) {

		if (tupleExpr instanceof StatementSourcePattern) {
			return estimateCost((StatementSourcePattern) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof ExclusiveStatement) {
			return estimateCost((ExclusiveStatement) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof ExclusiveGroup) {
			return estimateCost((ExclusiveGroup) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof NJoin) {
			return estimateCost((NJoin) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof NUnion) {
			return estimateCost((NUnion) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof FedXService) {
			return estimateCost((FedXService) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof Projection) {
			return estimateCost((Projection) tupleExpr, joinVars);
		}
		if (tupleExpr instanceof BindingSetAssignment) {
			return 0;
		}
		if (tupleExpr instanceof Extension) {
			return 0;
		}
		if (tupleExpr instanceof ArbitraryLengthPath) {
			return estimateCost((ArbitraryLengthPath) tupleExpr, joinVars);
		}

		log.debug("No cost estimation for " + tupleExpr.getClass().getSimpleName() + " available.");

		return 1000d;
	}

	private double estimateCost(ExclusiveGroup group, Set<String> joinVars) {

		// special heuristic: if not ordered at first place (i.e. there is a join var)
		// use the same counting technique as for others
		if (joinVars.size() > 0) {
			int count = 0;
			for (String var : group.getFreeVars()) {
				if (!joinVars.contains(var)) {
					count++;
				}
			}
			return 100 + (count / group.getExclusiveExpressions().size());
		}

		// heuristic: a group has a selective statement, if one statement has 0 or 1 free variables
		// if it has a selective statement, the group is executed as early as possible
		boolean hasSelectiveStatement = false;

		// TODO maybe add additional cost factor for ?x rdf:type :someType

		for (ExclusiveTupleExpr stmt : group.getExclusiveExpressions()) {
			if (stmt.getFreeVarCount() <= 1) {
				hasSelectiveStatement = true;
			}
		}

		double additionalCost = 0;

		// add some slight cost factor, if the subject in the query is not the same
		additionalCost += computeAdditionPatternCost(group.getExclusiveExpressions());

		if (!hasSelectiveStatement) {
			additionalCost += 4;
		}

//		if (hasExpensiveType)
//			additionalCost += 1;

		return 0 + additionalCost + (group.getFreeVarCount() / group.getExclusiveExpressions().size());

	}

	/**
	 * If the subject is not the same for all triple patterns in the group, an additional cost of .5 is considered.
	 *
	 * <p>
	 * Example:
	 * </p>
	 *
	 * <pre>
	 * ?x p o . ?x p2 o2 => cost is 0
	 *
	 * ?x p ?s . ?s ?p2 val => additional cost is 0.5
	 * </pre>
	 *
	 * @return
	 */
	private double computeAdditionPatternCost(List<ExclusiveTupleExpr> stmts) {

		String s = null;
		for (ExclusiveTupleExpr st : stmts) {
			if (!(st instanceof ExclusiveStatement)) {
				return 0.5;
			}
			String currentVar = QueryStringUtil.toString(((ExclusiveStatement) st).getSubjectVar());
			if (!currentVar.equals(s) && s != null) {
				return 0.5;
			}
			s = currentVar;
		}
		return 0.0;
	}

	private double estimateCost(ExclusiveStatement owned, Set<String> joinVars) {

		/* currently the cost is the number of free vars that are executed in the join */

		int count = 100;

		// special heuristic: if exclusive statement with one free variable, more selective than owned group
		// with more than 3 free variable
		if (owned.getFreeVarCount() <= 1 && joinVars.isEmpty()) {
			count = 3;
		}

		for (String var : owned.getFreeVars()) {
			if (!joinVars.contains(var)) {
				count++;
			}
		}

		return count;
	}

	private double estimateCost(FedXService service, Set<String> joinVars) {

		int additionalCost = 0;

		// evaluate services with variable service ref late (since the service ref
		// may be computed at evaluation time)
		if (!service.getService().getServiceRef().hasValue()) {
			additionalCost += 1000;
		}

		if (service.getNumberOfTriplePatterns() <= 1) {
			if (joinVars.isEmpty() && service.getFreeVarCount() <= 1) {
				additionalCost = 3; // compare exclusive statement
			} else {
				additionalCost += 100; // compare all statements
			}
		}

		return 0 + additionalCost + service.getFreeVarCount();
	}

	private double estimateCost(Projection projection, Set<String> joinVars) {

		// cost estimator for sub query

		return 0 + projection.getBindingNames().size();
	}

	private double estimateCost(StatementSourcePattern stmt, Set<String> joinVars) {

		/* currently the cost is the number of free vars that are executed in the join */

		int count = 100;
		for (String var : stmt.getFreeVars()) {
			if (!joinVars.contains(var)) {
				count++;
			}
		}

		return count;
	}

	private double estimateCost(NUnion nunion, Set<String> joinVars) {

		// the unions cost is determined is determined by the minimum cost
		// of the children + penalty cost of number of arguments
		double min = Double.MAX_VALUE;
		for (TupleExpr t : nunion.getArgs()) {
			double cost = estimateCost(t, joinVars);
			if (cost < min) {
				min = cost;
			}
		}

		return min + nunion.getNumberOfArguments() - 1;
	}

	private double estimateCost(NJoin join, Set<String> joinVars) {

		// cost of a join is determined by the cost of the first join arg
		// Note: the join order of this join is already determined (depth first)
		// in addition we add a penalty for the number of join arguments
		double cost = estimateCost(join.getArg(0), joinVars);

		return cost + join.getNumberOfArguments() - 1;
	}

	private double estimateCost(ArbitraryLengthPath path, Set<String> joinVars) {

		/* currently the cost is the number of free vars that are executed in the join */

		int count = 100;
		for (String var : QueryAlgebraUtil.getFreeVars(path.getPathExpression())) {
			if (!joinVars.contains(var)) {
				count++;
			}
		}

		return count;
	}
}
