package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class AggregatedValidationTuple extends ValidationTuple {

	List<Value> aggregatedFrom = new ArrayList<>();

	public AggregatedValidationTuple(Deque<Value> targetChain, Path path, Value value) {
		super(targetChain, path, value);
	}

	public AggregatedValidationTuple(AggregatedValidationTuple validationTuple) {
		super(validationTuple);
	}

	public void addAggregate(Value value) {
		aggregatedFrom.add(value);
	}

	@Override
	public Value getAnyValue() {
		return aggregatedFrom.get(0);
	}
}
