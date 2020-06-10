package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes;

import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;

public class ValidationTuple {

	private final List<Value> targetChain;
	private final Path path;
	private final Value value;

	static ValueComparator valueComparator = new ValueComparator();

	public boolean sameTargetAs(ValidationTuple nextRight) {
		return targetChain.equals(nextRight.targetChain);
	}

	public List<Value> getTargetChain() {
		return targetChain;
	}

	public Path getPath() {
		return path;
	}

	public Value getValue() {
		return value;
	}

	public ValidationTuple(List<Value> targetChain, Path path, Value value) {
		this.targetChain = targetChain;
		this.path = path;
		this.value = value;
	}

	public int compareTarget(ValidationTuple nextRight) {

		Value left = targetChain.get(targetChain.size() - 1);
		Value right = nextRight.targetChain.get(nextRight.targetChain.size() - 1);

		return valueComparator.compare(left, right);

	}
}
