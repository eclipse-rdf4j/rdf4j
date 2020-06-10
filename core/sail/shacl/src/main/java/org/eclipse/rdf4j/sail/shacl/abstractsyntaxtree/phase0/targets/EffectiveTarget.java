package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.ArrayDeque;

import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public class EffectiveTarget {

	private final ArrayDeque<Object> chain;

	public EffectiveTarget(ArrayDeque<Object> chain) {
		this.chain = chain;
	}

	public TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup) {
		if (chain.size() == 1) {
			// simple chain

			Object last = chain.getLast();
			if (last instanceof Target) {
				return ((Target) last).getAdded(connectionsGroup);
			} else {
				throw new UnsupportedOperationException(
						"Unknown target in chain is typo: " + last.getClass().getSimpleName());
			}

		} else {
			// complex chain
			throw new UnsupportedOperationException();
		}

	}
}
