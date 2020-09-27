package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationTuple {

	private static final Logger logger = LoggerFactory.getLogger(ValidationTuple.class);

	static ValueComparator valueComparator = new ValueComparator();
	private final Deque<Value> chain;
	private ConstraintComponent.Scope scope;
	private boolean propertyShapeScopeWithValue;

	Deque<ValidationResult> validationResults = new ArrayDeque<>();

	public ValidationTuple(ValidationTuple validationTuple) {
		this.chain = new ArrayDeque<>(validationTuple.chain);
		this.scope = validationTuple.scope;
		this.propertyShapeScopeWithValue = validationTuple.propertyShapeScopeWithValue;

		if (validationTuple.validationResults != null) {
			this.validationResults = new ArrayDeque<>(validationTuple.validationResults);
		}

	}

	public ValidationTuple(BindingSet bindingSet, String[] variables, ConstraintComponent.Scope scope,
			boolean hasValue) {
		this(bindingSet, Arrays.asList(variables), scope, hasValue);
	}

	public ValidationTuple(BindingSet bindingSet, List<String> variables, ConstraintComponent.Scope scope,
			boolean hasValue) {
		chain = new ArrayDeque<>();
		for (String variable : variables) {
			chain.addLast(bindingSet.getValue(variable));
		}
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;

	}

	public ValidationTuple(Deque<Value> targets, ConstraintComponent.Scope scope, boolean hasValue) {
		chain = targets;
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
	}

	public ValidationTuple(Value a, Value c, ConstraintComponent.Scope scope, boolean hasValue) {
		chain = new ArrayDeque<>();
		chain.addLast(a);
		chain.addLast(c);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
	}

	public ValidationTuple(Value subject, ConstraintComponent.Scope scope, boolean hasValue) {
		chain = new ArrayDeque<>();
		chain.addLast(subject);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
	}

	public boolean sameTargetAs(ValidationTuple other) {

		Value current = getActiveTarget();
		Value currentRight = other.getActiveTarget();

		return current.equals(currentRight);

	}

	public boolean hasValue() {
		assert scope != null;
		return propertyShapeScopeWithValue || scope == ConstraintComponent.Scope.nodeShape;
	}

	public Value getValue() {
		assert scope != null;
		if (hasValue()) {
			return chain.peekLast();
		}

		return null;
	}

	public ConstraintComponent.Scope getScope() {
		return scope;
	}

	public void setScope(ConstraintComponent.Scope scope) {
		assert this.scope == null;
		this.scope = scope;
	}

	public int compareTarget(ValidationTuple other) {

		Value left = getActiveTarget();
		Value right = other.getActiveTarget();

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
		assert scope != null;
		if (!propertyShapeScopeWithValue || scope != ConstraintComponent.Scope.propertyShape) {
			return chain.getLast();
		}

		if (chain.size() < 2) {
			throw new AssertionError(chain.size());
		}

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
		return propertyShapeScopeWithValue == that.propertyShapeScopeWithValue &&
				Objects.equals(new ArrayList<>(chain), new ArrayList<>(that.chain)) &&
				scope == that.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(new ArrayList<>(chain), scope, propertyShapeScopeWithValue);
	}

	@Override
	public String toString() {
		return "ValidationTuple{" +
				"chain=" + Arrays.toString(chain.toArray()) +
				", scope=" + scope +
				", propertyShapeScopeWithValue=" + propertyShapeScopeWithValue +
				'}';
	}

	public void shiftToNodeShape() {
		assert scope == ConstraintComponent.Scope.propertyShape;
		if (propertyShapeScopeWithValue) {
			propertyShapeScopeWithValue = false;
			chain.removeLast();
		}
		scope = ConstraintComponent.Scope.nodeShape;
	}

	public void shiftToPropertyShapeScope() {
		assert scope == ConstraintComponent.Scope.nodeShape;
		assert chain.size() >= 2;
		scope = ConstraintComponent.Scope.propertyShape;
		propertyShapeScopeWithValue = true;
	}

	public int getFullChainSize() {
		return chain.size();
	}

	/**
	 * This is only the target part. For property shape scope it will not include the value.
	 *
	 * @return
	 * @param includePropertyShapeValues
	 */
	public Collection<Value> getTargetChain(boolean includePropertyShapeValues) {

		if (scope == ConstraintComponent.Scope.propertyShape && hasValue() && !includePropertyShapeValues) {
			return chain.stream().limit(chain.size() - 1).collect(Collectors.toList());
		}

		return new ArrayList<>(chain);
	}

	public void setValue(Value value) {
		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {
				chain.removeLast();
			}
			chain.addLast(value);
		} else {
			chain.removeLast();
			chain.addLast(value);

		}

		propertyShapeScopeWithValue = true;
	}

	public int compareValue(ValidationTuple other) {
		Value left = getValue();
		Value right = other.getValue();

		return valueComparator.compare(left, right);
	}

	public void trimToTarget() {
		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {
				chain.removeLast();
				propertyShapeScopeWithValue = false;
			}
		}
	}

	public void pop() {
		assert chain.size() > 1 : "Attempting to pop chain will not leave any elements on the chain! " + toString();

		chain.removeLast();
	}
}
