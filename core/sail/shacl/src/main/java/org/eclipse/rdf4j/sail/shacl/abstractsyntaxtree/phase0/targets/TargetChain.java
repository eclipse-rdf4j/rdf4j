package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;

public class TargetChain {

	private final ArrayDeque<Object> chain = new ArrayDeque<>();

	private boolean optimizable = true;

	public TargetChain() {
	}

	public TargetChain(TargetChain targetChain) {
		optimizable = targetChain.optimizable;
	}

	public TargetChain add(Object o) {

		TargetChain targetChain = new TargetChain(this);
		targetChain.chain.addAll(this.chain);
		targetChain.chain.addLast(o);

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

	public Collection<Object> getChain() {
		return Collections.unmodifiableCollection(chain);
	}

	public boolean isOptimizable() {
		return optimizable;
	}

	public Optional<Path> getPath() {
		Object last = chain.getLast();

		if (last instanceof Path) {
			return Optional.of((Path) last);
		}

		return Optional.empty();
	}

	public EffectiveTarget getEffectiveTarget() {

		ArrayDeque<Object> newChain = new ArrayDeque<>(chain);
		newChain.removeLast();

		return new EffectiveTarget(newChain);
	}

}
