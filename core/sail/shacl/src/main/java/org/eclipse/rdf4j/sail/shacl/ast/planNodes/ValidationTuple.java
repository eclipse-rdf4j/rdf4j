package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationTuple {

	private static final Logger logger = LoggerFactory.getLogger(ValidationTuple.class);

	private static final ValueComparator valueComparator = new ValueComparator();
	private final List<Value> chain;
	private final ConstraintComponent.Scope scope;
	private final boolean propertyShapeScopeWithValue;

	Deque<ValidationResult> validationResults = new ArrayDeque<>();

	public ValidationTuple(ValidationTuple validationTuple) {
		this.chain = validationTuple.chain;
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
		List<Value> chain = new ArrayList<>();
		for (String variable : variables) {
			chain.add(bindingSet.getValue(variable));
		}
		this.chain = Collections.unmodifiableList(chain);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;

	}

	public ValidationTuple(List<Value> targets, ConstraintComponent.Scope scope, boolean hasValue) {
		this.chain = Collections.unmodifiableList(targets);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
	}

	public ValidationTuple(Value a, Value c, ConstraintComponent.Scope scope, boolean hasValue) {
		this.chain = Collections.unmodifiableList(Arrays.asList(a, c));
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
	}

	public ValidationTuple(Value subject, ConstraintComponent.Scope scope, boolean hasValue) {

		this.chain = Collections.singletonList(subject);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
	}

	public ValidationTuple(Deque<ValidationResult> validationResults, List<Value> chain,
			ConstraintComponent.Scope scope, boolean propertyShapeScopeWithValue) {
		if (validationResults != null) {
			this.validationResults = new ArrayDeque<>(validationResults);
		}
		this.chain = Collections.unmodifiableList(chain);
		this.scope = scope;
		this.propertyShapeScopeWithValue = propertyShapeScopeWithValue;
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
			return chain.get(chain.size() - 1);
		}

		return null;
	}

	public ConstraintComponent.Scope getScope() {
		return scope;
	}

	public int compareActiveTarget(ValidationTuple other) {

		Value left = getActiveTarget();
		Value right = other.getActiveTarget();

		return valueComparator.compare(left, right);
	}

	public int compareFullTarget(ValidationTuple other) {

		int min = Math.min(getFullChainSize(false), other.getFullChainSize(false));

		Collection<Value> targetChain = getTargetChain(false);
		ArrayList<Value> otherTargetChain = new ArrayList<>(other.getTargetChain(false));

		Iterator<Value> iterator = targetChain.iterator();

		for (int i = 0; i < min; i++) {
			Value value = iterator.next();
			int compare = valueComparator.compare(value, otherTargetChain.get(i));
			if (compare != 0) {
				return compare;
			}
		}

		return Integer.compare(getFullChainSize(true), other.getFullChainSize(true));
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
			return chain.get(chain.size() - 1);
		}

		if (chain.size() < 2) {
			throw new AssertionError(chain.size());
		}

		return chain.get(chain.size() - 2);

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

	public ValidationTuple shiftToNodeShape() {
		assert scope == ConstraintComponent.Scope.propertyShape;

		ArrayList<Value> chain = new ArrayList<>(this.chain);
		boolean propertyShapeScopeWithValue = this.propertyShapeScopeWithValue;
		ConstraintComponent.Scope scope = this.scope;

		if (this.propertyShapeScopeWithValue) {
			propertyShapeScopeWithValue = false;
			chain.remove(chain.size() - 1);
		}
		scope = ConstraintComponent.Scope.nodeShape;

		return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue);

	}

	public ValidationTuple shiftToPropertyShapeScope() {
		assert scope == ConstraintComponent.Scope.nodeShape;
		assert chain.size() >= 2;

		ArrayList<Value> chain = new ArrayList<>(this.chain);
		boolean propertyShapeScopeWithValue = true;
		ConstraintComponent.Scope scope = ConstraintComponent.Scope.propertyShape;

		return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue);

	}

	public int getFullChainSize(boolean includePropertyShapeValue) {
		if (!includePropertyShapeValue && propertyShapeScopeWithValue) {
			return chain.size() - 1;
		}

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

	public ValidationTuple setValue(Value value) {

		List<Value> chain = new ArrayList<>(this.chain);

		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {
				chain.remove(this.chain.size() - 1);
			}
			chain.add(value);
		} else {
			chain.remove(this.chain.size() - 1);
			chain.add(value);

		}

		boolean propertyShapeScopeWithValue = true;
		return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue);
	}

	public int compareValue(ValidationTuple other) {
		Value left = getValue();
		Value right = other.getValue();

		return valueComparator.compare(left, right);
	}

	public ValidationTuple trimToTarget() {
		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {
				List<Value> chain = new ArrayList<>(this.chain);
				chain.remove(this.chain.size() - 1);
				boolean propertyShapeScopeWithValue = false;
				return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue);
			}
		}
		return this;
	}

	public ValidationTuple pop() {
		List<Value> chain = new ArrayList<>(this.chain);
		boolean propertyShapeScopeWithValue = this.propertyShapeScopeWithValue;
		if (getScope() == ConstraintComponent.Scope.propertyShape) {
			if (hasValue()) {
				assert chain.size() > 1 : "Attempting to pop chain will not leave any elements on the chain! "
						+ toString();
				chain.remove(chain.size() - 1);
			} else {
				propertyShapeScopeWithValue = true;
			}
		} else {
			assert chain.size() > 1 : "Attempting to pop chain will not leave any elements on the chain! " + toString();
			chain.remove(chain.size() - 1);
		}

		return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue);

	}
}
