/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.EmptyNJoin;
import org.eclipse.rdf4j.federated.algebra.EmptyResult;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.TrueStatementPattern;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
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
 * {@link JoinOrderOptimizer}
 * 
 * 
 * @author as
 */
public class StatementGroupOptimizer extends AbstractQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	private static final Logger log = LoggerFactory.getLogger(StatementGroupOptimizer.class);

	protected final QueryInfo queryInfo;

	public StatementGroupOptimizer(QueryInfo queryInfo) {
		super();
		this.queryInfo = queryInfo;
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

		LinkedList<TupleExpr> newArgs = new LinkedList<>();

		LinkedList<TupleExpr> argsCopy = new LinkedList<>(node.getArgs());
		while (!argsCopy.isEmpty()) {

			TupleExpr t = argsCopy.removeFirst();

			/*
			 * If one of the join arguments cannot produce results, the whole join expression does not produce results.
			 * => replace with empty join and return
			 */
			if (t instanceof EmptyResult) {
				node.replaceWith(new EmptyNJoin(node, queryInfo));
				return;
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

			/*
			 * statement yields true in any case, not needed for join
			 */
			else if (t instanceof TrueStatementPattern) {
				if (log.isDebugEnabled())
					log.debug("Statement " + QueryStringUtil.toString((StatementPattern) t)
							+ " yields results for at least one provided source, prune it.");
			}

			else
				newArgs.add(t);
		}

		// if the join args could be reduced to just one, e.g. OwnedGroup
		// we can safely replace the join node
		if (newArgs.size() == 1) {
			log.debug("Join arguments could be reduced to a single argument, replacing join node.");
			node.replaceWith(newArgs.get(0));
			return;
		}

		// in rare cases the join args can be reduced to 0, e.g. if all statements are
		// TrueStatementPatterns. We can safely replace the join node in such case
		if (newArgs.isEmpty()) {
			log.debug("Join could be pruned as all join statements evaluate to true, replacing join with true node.");
			node.replaceWith(new TrueStatementPattern(new StatementPattern()));
			return;
		}

		List<TupleExpr> optimized = newArgs;

		// optimize the join order
		optimized = JoinOrderOptimizer.optimizeJoinOrder(optimized);

		// exchange the node
		NJoin newNode = new NJoin(optimized, queryInfo);
		node.replaceWith(newNode);
	}

}
