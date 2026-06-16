package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.EmptyNJoin;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.TripleRefJoinGroup;
import org.eclipse.rdf4j.federated.algebra.TripleRefStatementPattern;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimizer that groups {@link TripleRefStatementPattern} nodes with co-located {@link StatementPattern} nodes into
 * {@link TripleRefJoinGroup} instances, enabling efficient federated evaluation of RDF 1.2 reification patterns.
 *
 * <p>
 * The optimizer visits every {@link NJoin} in the query tree (depth-first) and looks for pairs of a
 * {@link TripleRefStatementPattern} and one or more plain {@link StatementPattern}s that share the same unbound subject
 * variable. Matched patterns are collapsed into a single {@link TripleRefJoinGroup} that can be sent as one request to
 * the owning endpoints.
 * </p>
 *
 * @author Andreas Schwarte
 * @see TripleRefJoinGroup
 * @see TripleRefStatementPattern
 */
public class TripleRefJoinOptimizer extends AbstractSimpleQueryModelVisitor<OptimizationException>
		implements FedXOptimizer {

	private static final Logger log = LoggerFactory.getLogger(TripleRefJoinOptimizer.class);

	protected final QueryInfo queryInfo;

	public TripleRefJoinOptimizer(QueryInfo queryInfo) {
		super(true);
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
	}

	/**
	 * Group {@link TripleRefStatementPattern} and {@link StatementPattern} having the same subject variable into a
	 * {@link TripleRefJoinGroup}.
	 *
	 * <p>
	 * <b>Assumption:</b> a shared unbound subject variable is treated as a sufficient co-location signal. In RDF 1.2
	 * reification patterns the subject linking the outer statement to its triple term is typically a blank node that is
	 * local to a single dataset (and therefore to a single endpoint). No additional source-exclusivity check is
	 * performed.
	 * </p>
	 *
	 * @param originalArgs the join arguments to group
	 * @return the new (potentially grouped) join arguments. If empty, the join will not produce any results.
	 */
	protected List<TupleExpr> formGroups(List<TupleExpr> originalArgs) {

		LinkedList<TupleExpr> newArgs = new LinkedList<>();
		LinkedList<TupleExpr> argsCopy = new LinkedList<>(originalArgs);

		for (TupleExpr te : originalArgs) {

			if (!(te instanceof TripleRefStatementPattern tref)) {
				continue;
			}

			if (tref.getSubjectVar().hasValue()) {
				continue;
			}

			// guard: skip if already claimed by a group formed in an earlier iteration
			if (!argsCopy.contains(tref)) {
				continue;
			}

			String varName = tref.getSubjectVar().getName();
			List<StatementSource> trefSources = tref.getStatementSources();

			List<StatementPattern> group = new ArrayList<>();
			// find StatementPatterns (but not further TripleRefStatementPatterns) sharing the subject variable
			for (TupleExpr te2 : argsCopy) {
				if (te2 instanceof StatementPattern st
						&& !(te2 instanceof TripleRefStatementPattern)
						&& !st.getSubjectVar().hasValue()
						&& st.getSubjectVar().getName().equals(varName)) {
					group.add(st);
				}
			}

			if (!group.isEmpty()) {
				TripleRefJoinGroup trGroup = new TripleRefJoinGroup(tref, group, trefSources, queryInfo);
				argsCopy.remove(tref);
				argsCopy.removeAll(group);
				newArgs.add(trGroup);
			}
		}

		// add remaining
		newArgs.addAll(argsCopy);

		return newArgs;
	}
}
