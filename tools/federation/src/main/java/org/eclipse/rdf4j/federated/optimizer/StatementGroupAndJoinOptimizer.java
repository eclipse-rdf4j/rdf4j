/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.algebra.EmptyNJoin;
import org.eclipse.rdf4j.federated.algebra.EmptyResult;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.NTuple;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimizer with the following tasks:
 * 
 * 1. Group {@link ExclusiveStatement} into {@link ExclusiveGroup} 2. Adjust the join order using
 * {@link DefaultFedXCostModel}
 * 
 * 
 * @author as
 */
public class StatementGroupAndJoinOptimizer extends AbstractQueryModelVisitor<OptimizationException>
		implements FedXOptimizer {

	private static final Logger log = LoggerFactory.getLogger(StatementGroupAndJoinOptimizer.class);

	protected final QueryInfo queryInfo;

	private final FedXCostModel costModel;

	public StatementGroupAndJoinOptimizer(QueryInfo queryInfo, FedXCostModel costModel) {
		super();
		this.queryInfo = queryInfo;
		this.costModel = costModel;
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {
		tupleExpr.visit(this);
	}

	@Override
	public void meet(Service tupleExpr) {
		// stop traversal
	}

	@Override
	public void meetOther(QueryModelNode node) {
		if (node instanceof NJoin) {
			super.meetOther(node); // depth first
			meetNJoin((NJoin) node);
		} else {
			super.meetOther(node);
		}
	}

	protected void meetNJoin(NJoin node) {

		List<TupleExpr> args = node.getArgs();

		// form groups
		args = formGroups(args);

		if (args.isEmpty()) {
			node.replaceWith(new EmptyNJoin(node, queryInfo));
			return;
		}

		// if the join args could be reduced to just one, e.g. ExclusiveGroup
		// we can safely replace the join node
		if (args.size() == 1) {
			log.debug("Join arguments could be reduced to a single argument, replacing join node.");
			node.replaceWith(args.get(0));
			return;
		}

		// optimize the join order
		args = optimizeJoinOrder(args);

		// exchange the node
		NJoin newNode = new NJoin(args, queryInfo);
		node.replaceWith(newNode);
	}

	/**
	 * Group {@link ExclusiveStatement}s having the same source into an {@link ExclusiveGroup}.
	 * 
	 * @param originalArgs
	 * @return the new (potentially grouped) join arguments. If empty, the join will not produce any results.
	 */
	protected List<TupleExpr> formGroups(List<TupleExpr> originalArgs) {

		LinkedList<TupleExpr> newArgs = new LinkedList<>();

		LinkedList<TupleExpr> argsCopy = new LinkedList<>(originalArgs);
		while (!argsCopy.isEmpty()) {

			TupleExpr t = argsCopy.removeFirst();

			/*
			 * If one of the join arguments cannot produce results, the whole join expression does not produce results.
			 * => return an null as marker to replace with empty join
			 */
			if (t instanceof EmptyResult) {
				return Collections.emptyList();
			}

			/*
			 * for exclusive statements find those belonging to the same source (if any) and form exclusive group
			 */
			else if (t instanceof ExclusiveStatement) {
				ExclusiveStatement current = (ExclusiveStatement) t;

				List<ExclusiveStatement> l = null;
				List<ExclusiveGroup> toRemoveFromArgs = null; // contains exclusive groups have to be removed
				for (TupleExpr te : argsCopy) {
					/*
					 * in the remaining join args find exclusive statements having the same source, and add to a list
					 * which is later used to form an exclusive group
					 */
					if (te instanceof ExclusiveStatement) {
						ExclusiveStatement check = (ExclusiveStatement) te;
						if (check.getOwner().equals(current.getOwner())) {
							if (l == null) {
								l = new ArrayList<>();
								l.add(current);
							}
							l.add(check);
						}
					}
					/*
					 * also scan for exclusive groups having the same owner
					 */
					else if (te instanceof ExclusiveGroup) {
						ExclusiveGroup check = (ExclusiveGroup) te;
						if (check.getOwner().equals(current.getOwner())) {
							if (l == null) {
								l = new ArrayList<>();
								l.add(current);
							}
							if (toRemoveFromArgs == null) {
								toRemoveFromArgs = new ArrayList<>();
							}
							toRemoveFromArgs.add(check);
							l.addAll(check.getStatements());
						}
					}
				}

				// check if we can construct a group, otherwise add directly
				if (l != null) {
					argsCopy.removeAll(l);
					if (toRemoveFromArgs != null) {
						argsCopy.removeAll(toRemoveFromArgs);
					}
					newArgs.add(new ExclusiveGroup(l, current.getOwner(), queryInfo));
				} else {
					newArgs.add(current);
				}
			}

			/*
			 * for (existing) exclusive groups (e.g. created by SERVICE clauses) add potential ExclusiveStatements with
			 * the same source to the group
			 */
			else if (t instanceof ExclusiveGroup) {

				ExclusiveGroup current = (ExclusiveGroup) t;

				List<ExclusiveStatement> l = null;
				for (TupleExpr te : argsCopy) {
					/*
					 * in the remaining join args find exclusive statements having the same source, and add to a list
					 * which is later used to form an exclusive group
					 */
					if (te instanceof ExclusiveStatement) {
						ExclusiveStatement check = (ExclusiveStatement) te;
						if (check.getOwner().equals(current.getOwner())) {
							if (l == null) {
								l = new ArrayList<>();
								l.addAll(current.getStatements());
							}
							l.add(check);
						}
					}
				}

				// check if we have a modification, otherwise add existing node
				if (l != null) {
					argsCopy.removeAll(l);
					argsCopy.remove(current);
					newArgs.add(new ExclusiveGroup(l, current.getOwner(), queryInfo));
				} else {
					newArgs.add(current);
				}

			}

			else {
				newArgs.add(t);
			}
		}

		return newArgs;
	}

	/**
	 * Join Order Optimizer
	 * 
	 * Group -> Statements according to number of free Variables
	 * 
	 * Additional Heuristics: - ExclusiveGroups are cheaper than any other subquery - owned statements are cheaper if
	 * they have a single free variable
	 * 
	 * @param joinArgs
	 * @return
	 */
	protected List<TupleExpr> optimizeJoinOrder(List<TupleExpr> joinArgs) {

		List<TupleExpr> optimized = new ArrayList<>(joinArgs.size());
		List<TupleExpr> left = new LinkedList<>(joinArgs);
		Set<String> joinVars = new HashSet<>();

		while (!left.isEmpty()) {

			TupleExpr item = left.get(0);

			double minCost = Double.MAX_VALUE;
			for (TupleExpr tmp : left) {

				double currentCost = estimateCost(tmp, joinVars);
				if (currentCost < minCost) {
					item = tmp;
					minCost = currentCost;
				}
			}

			joinVars.addAll(getFreeVars(item));
			if (log.isTraceEnabled())
				log.trace("Cost of " + item.getClass().getSimpleName() + " is determined as " + minCost);
			optimized.add(item);
			left.remove(item);
		}

		return optimized;
	}

	protected double estimateCost(TupleExpr tupleExpr, Set<String> joinVars) {
		return costModel.estimateCost(tupleExpr, joinVars);
	}

	public static Collection<String> getFreeVars(TupleExpr tupleExpr) {
		if (tupleExpr instanceof StatementTupleExpr)
			return ((StatementTupleExpr) tupleExpr).getFreeVars();

		// determine the number of free variables in a UNION or Join
		if (tupleExpr instanceof NTuple) {
			HashSet<String> freeVars = new HashSet<>();
			NTuple ntuple = (NTuple) tupleExpr;
			for (TupleExpr t : ntuple.getArgs())
				freeVars.addAll(getFreeVars(t));
			return freeVars;
		}

		if (tupleExpr instanceof FedXService) {
			return ((FedXService) tupleExpr).getFreeVars();
		}

		if (tupleExpr instanceof Service) {
			return ((Service) tupleExpr).getServiceVars();
		}

		// can happen in SERVICE nodes, if they cannot be optimized
		if (tupleExpr instanceof StatementPattern) {
			List<String> freeVars = new ArrayList<>();
			StatementPattern st = (StatementPattern) tupleExpr;
			if (st.getSubjectVar().getValue() == null)
				freeVars.add(st.getSubjectVar().getName());
			if (st.getPredicateVar().getValue() == null)
				freeVars.add(st.getPredicateVar().getName());
			if (st.getObjectVar().getValue() == null)
				freeVars.add(st.getObjectVar().getName());
			return freeVars;
		}

		if (tupleExpr instanceof Projection) {
			Projection p = (Projection) tupleExpr;
			return new ArrayList<>(p.getBindingNames());
		}

		if (tupleExpr instanceof BindingSetAssignment) {
			return new ArrayList<>();
		}

		if (tupleExpr instanceof Extension) {
			// for a BIND extension in our cost model we work with 0 free vars
			return new ArrayList<String>();
		}

		throw new FedXRuntimeException("Type " + tupleExpr.getClass().getSimpleName()
				+ " not supported for cost estimation. If you run into this, please report a bug.");

	}
}
