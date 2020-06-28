package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Targetable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;

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
				return target.getQueryFragment(null, var);
			} else {
				return target.getQueryFragment(prev.var, var);
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
		if (chain.size() == 1) {
			// simple chain

			EffectiveTargetObject last = chain.getLast();
			if (last.target instanceof Target) {
				return ((Target) last.target).getAdded(connectionsGroup);
			} else {
				throw new UnsupportedOperationException(
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
		if (chain.size() == 1) {
			// simple chain

			EffectiveTargetObject last = chain.getLast();
			if (last.target instanceof Target) {
				return ((Target) last.target).getTargetFilter(connectionsGroup, parent);
			} else {
				throw new UnsupportedOperationException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}

		}

		throw new UnsupportedOperationException();
	}

	public String getQuery() {

		String query = chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

		return query;

	}

}
