package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.ArrayList;
import java.util.List;

public class TargetChain {

	private final List<Object> chain = new ArrayList<>();

	private boolean optimizable = true;

	public TargetChain() {
	}

	public TargetChain(TargetChain targetChain) {
		optimizable = targetChain.optimizable;
	}

	public TargetChain add(Object o) {

		TargetChain targetChain = new TargetChain(this);
		targetChain.chain.addAll(this.chain);
		targetChain.chain.add(o);

		return targetChain;
	}

	public TargetChain setOptimizable(boolean optimizable) {
		TargetChain targetChain = new TargetChain(this);
		targetChain.chain.addAll(this.chain);

		if (targetChain.optimizable) {
			targetChain.optimizable = optimizable;
		}

		return targetChain;
	}

}
