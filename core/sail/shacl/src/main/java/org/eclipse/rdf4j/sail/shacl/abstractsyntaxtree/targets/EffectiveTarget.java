package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Targetable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;

public class EffectiveTarget {

	public Var getTargetVar() {
		return chain.getLast().var;
	}

	static class EffectiveTargetObject {

		final Var var;
		final Targetable target;
		final EffectiveTargetObject prev;

		public EffectiveTargetObject(Var var, Targetable target, EffectiveTargetObject prev) {
			this.var = var;
			this.target = target;
			this.prev = prev;
		}

		public Stream<StatementPattern> getStatementPatterns() {
			if (prev == null) {
				return target.getStatementPatterns(null, var);
			} else {
				return target.getStatementPatterns(prev.var, var);
			}
		}

		public String getQueryFragment() {
			if (prev == null) {
				return target.getTargetQueryFragment(null, var);
			} else {
				return target.getTargetQueryFragment(prev.var, var);
			}
		}
	}

	private final ArrayDeque<EffectiveTargetObject> chain;

	public EffectiveTarget(ArrayDeque<Targetable> chain, String targetVarPrefix) {
		int index = 0;

		this.chain = new ArrayDeque<>();
		EffectiveTargetObject prev = null;
		for (Targetable o : chain) {
			EffectiveTargetObject effectiveTargetObject = new EffectiveTargetObject(
					new Var(targetVarPrefix + String.format("%010d", index++)), o,
					prev);
			prev = effectiveTargetObject;
			this.chain.addLast(effectiveTargetObject);
		}

	}

	public PlanNode getAdded(ConnectionsGroup connectionsGroup) {
		assert !chain.isEmpty();

		if (chain.size() == 1) {
			// simple chain

			EffectiveTargetObject last = chain.getLast();
			if (last.target instanceof Target) {
				return ((Target) last.target).getAdded(connectionsGroup);
			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}

		} else {
			// complex chain

			List<StatementPattern> collect = chain.stream()
					.flatMap(EffectiveTargetObject::getStatementPatterns)
					.collect(Collectors.toList());

			String query = chain.stream()
					.map(EffectiveTargetObject::getQueryFragment)
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			return new TargetChainRetriever(connectionsGroup.getAddedStatements(), connectionsGroup.getBaseConnection(),
					collect, query);

		}

	}

	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {

		EffectiveTargetObject last = chain.getLast();

		if (chain.size() == 1) {
			// simple chain

			if (last.target instanceof Target) {
				return ((Target) last.target).getTargetFilter(connectionsGroup, parent);
			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}
		}

		String query = chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), parent, query, last.var,
				ValidationTuple::getActiveTarget)
						.getTrueNode(UnBufferedPlanNode.class);
	}

	public String getQuery() {

		return chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

	}

}
