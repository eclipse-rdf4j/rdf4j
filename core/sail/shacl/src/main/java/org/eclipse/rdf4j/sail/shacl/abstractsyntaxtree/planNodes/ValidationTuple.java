package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationTuple {

	private static final Logger logger = LoggerFactory.getLogger(ValidationTuple.class);

	static ValueComparator valueComparator = new ValueComparator();
	private final Deque<Value> chain;

	Deque<ValidationResult> validationResults;

	private int focusNodeOffsetFromEnd = 0;

	public ValidationTuple(ValidationTuple validationTuple) {
		this.chain = new ArrayDeque<>(validationTuple.chain);
		this.focusNodeOffsetFromEnd = validationTuple.focusNodeOffsetFromEnd;

		if (validationTuple.validationResults != null) {
			this.validationResults = new ArrayDeque<>(validationTuple.validationResults);
		}
		assert this.focusNodeOffsetFromEnd >= 0;
	}

	public ValidationTuple(BindingSet bindingSet, String[] variables, int focusNodeOffsetFromEnd) {
		this(bindingSet, Arrays.asList(variables), focusNodeOffsetFromEnd);
	}

	public ValidationTuple(Value target) {
		chain = new ArrayDeque<>();
		chain.addLast(target);

	}

	public ValidationTuple(BindingSet bindingSet, List<String> variables, int focusNodeOffsetFromEnd) {
		chain = new ArrayDeque<>();
		for (String variable : variables) {
			chain.addLast(bindingSet.getValue(variable));
		}
		this.focusNodeOffsetFromEnd = focusNodeOffsetFromEnd;
		assert this.focusNodeOffsetFromEnd >= 0;
	}

	public ValidationTuple(Deque<Value> targets, int focusNodeOffsetFromEnd) {
		chain = targets;
		this.focusNodeOffsetFromEnd = focusNodeOffsetFromEnd;
	}

	public ValidationTuple(Value a, Value c, int focusNodeOffsetFromEnd) {
		chain = new ArrayDeque<>();
		chain.addLast(a);
		chain.addLast(c);
		this.focusNodeOffsetFromEnd = focusNodeOffsetFromEnd;
	}

	public ValidationTuple(Value subject, int focusNodeOffsetFromEnd) {
		chain = new ArrayDeque<>();
		chain.addLast(subject);
		this.focusNodeOffsetFromEnd = focusNodeOffsetFromEnd;
	}

	public boolean sameTargetAs(ValidationTuple nextRight) {

		Value current = getActiveTarget();
		Value currentRight = nextRight.getActiveTarget();

		return current.equals(currentRight);

	}

	public Deque<Value> getChain() {
		return chain;
	}

	public boolean hasValue() {
		return focusNodeOffsetFromEnd > 0;
	}

	public Value getValue() {
		if (focusNodeOffsetFromEnd > 1) {
			throw new UnsupportedOperationException();
		}

		if (focusNodeOffsetFromEnd == 1) {
			return chain.peekLast();
		}

		return null;
	}

	public void setFocusNodeOffsetFromEnd(int focusNodeOffsetFromEnd) {
		this.focusNodeOffsetFromEnd = focusNodeOffsetFromEnd;
	}

	public int compareTarget(ValidationTuple nextRight) {

		Value left = chain.getLast();
		Value right = nextRight.chain.getLast();

		return valueComparator.compare(left, right);
	}

	public Deque<ValidationResult> toValidationResult() {
		return validationResults;
	}

	public void addValidationResult(ValidationResult validationResult) {
		if (validationResults == null) {
			validationResults = new ArrayDeque<>();
		}
		this.validationResults.addFirst(validationResult);
	}

	public Value getActiveTarget() {
		if (focusNodeOffsetFromEnd == 0) {
			return chain.getLast();
		}

		assert focusNodeOffsetFromEnd == 1;

		Value value = chain.removeLast();
		Value last = chain.getLast();
		chain.addLast(value);
		return last;

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ValidationTuple that = (ValidationTuple) o;

		return chain.size() == that.chain.size()
				&& chain.containsAll(that.chain);
	}

	@Override
	public int hashCode() {
		return Objects.hash(chain.toArray());
	}

	@Override
	public String toString() {
		return "ValidationTuple{" +
				"chain=" + Arrays.toString(chain.toArray()) +
				", focusNodeOffsetFromEnd=" + focusNodeOffsetFromEnd +
				'}';
	}

	public void trimToTarget() {
		for (int i = 0; i < focusNodeOffsetFromEnd; i++) {
			chain.removeLast();
		}
		focusNodeOffsetFromEnd = 0;
	}
}
