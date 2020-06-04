package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.ArrayList;
import java.util.List;

public class TargetChain {

	List<Object> chain = new ArrayList<>();

	public TargetChain add(Object o) {

		TargetChain targetChain = new TargetChain();
		targetChain.chain.addAll(this.chain);
		targetChain.chain.add(o);

		return targetChain;
	}

}
