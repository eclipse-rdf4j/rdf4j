package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Value;

public class AggregatedValidationTuple extends ValidationTuple {

	List<Value> aggregatedFrom = new ArrayList<>();

	public AggregatedValidationTuple(AggregatedValidationTuple validationTuple) {
		super(validationTuple);
	}

	public AggregatedValidationTuple(ValidationTuple validationTuple) {
		super(validationTuple);
	}

	public void addAggregate(Value value) {
		aggregatedFrom.add(value);
	}

}
