package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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

	// all fields should be immutable
	private final List<Value> chain;
	private final ConstraintComponent.Scope scope;
	private final boolean propertyShapeScopeWithValue;
	private final List<ValidationResult> validationResults;
	private final Set<ValidationTuple> compressedTuples;

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
		this.validationResults = Collections.emptyList();
		this.compressedTuples = Collections.emptySet();
	}

	public ValidationTuple(List<Value> targets, ConstraintComponent.Scope scope, boolean hasValue) {
		this.chain = Collections.unmodifiableList(targets);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = Collections.emptyList();
		this.compressedTuples = Collections.emptySet();

	}

	public ValidationTuple(Value a, Value c, ConstraintComponent.Scope scope, boolean hasValue) {
		this.chain = Collections.unmodifiableList(Arrays.asList(a, c));
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = Collections.emptyList();
		this.compressedTuples = Collections.emptySet();

	}

	public ValidationTuple(Value subject, ConstraintComponent.Scope scope, boolean hasValue) {
		this.chain = Collections.singletonList(subject);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = Collections.emptyList();
		this.compressedTuples = Collections.emptySet();

	}

	private ValidationTuple(List<ValidationResult> validationResults, List<Value> chain,
			ConstraintComponent.Scope scope, boolean propertyShapeScopeWithValue,
			Set<ValidationTuple> compressedTuples) {
		this.validationResults = Collections.unmodifiableList(validationResults);
		this.chain = Collections.unmodifiableList(chain);
		this.scope = scope;
		this.propertyShapeScopeWithValue = propertyShapeScopeWithValue;
		this.compressedTuples = Collections.unmodifiableSet(compressedTuples);

	}

	public ValidationTuple(ValidationTuple temp, Set<ValidationTuple> compressedTuples) {
		this.validationResults = temp.validationResults;
		this.chain = temp.chain;
		this.scope = temp.scope;
		this.propertyShapeScopeWithValue = temp.propertyShapeScopeWithValue;
		this.compressedTuples = Collections.unmodifiableSet(compressedTuples);

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

	public List<ValidationResult> getValidationResult() {
		return validationResults;
	}

	public ValidationTuple addValidationResult(Function<ValidationTuple, ValidationResult> validationResult) {
		List<ValidationResult> validationResults;
		if (!this.validationResults.isEmpty()) {
			validationResults = new ArrayList<>(this.validationResults);
			validationResults.add(validationResult.apply(this));
		} else {
			validationResults = Collections.singletonList(validationResult.apply(this));
		}

		Set<ValidationTuple> compressedTuples = this.compressedTuples.stream()
				.map(t -> t.addValidationResult(validationResult))
				.collect(Collectors.toSet());

		return new ValidationTuple(validationResults, chain, scope, propertyShapeScopeWithValue, compressedTuples);
	}

	public Value getActiveTarget() {
		assert scope != null;
		if (!propertyShapeScopeWithValue || scope != ConstraintComponent.Scope.propertyShape) {
			return chain.get(chain.size() - 1);
		}

		assert chain.size() >= 2;

		return chain.get(chain.size() - 2);

	}

	@Override
	public String toString() {
		return "ValidationTuple{" +
				"chain=" + chain +
				", scope=" + scope +
				", propertyShapeScopeWithValue=" + propertyShapeScopeWithValue +
//			", validationResults=" + validationResults +
				", compressedTuples=" + Arrays.toString(compressedTuples.toArray()) +
				'}';
	}

	public List<ValidationTuple> shiftToNodeShape() {
		assert scope == ConstraintComponent.Scope.propertyShape;

		if (compressedTuples.isEmpty()) {
			List<Value> chain = this.chain;
			boolean propertyShapeScopeWithValue = this.propertyShapeScopeWithValue;
			ConstraintComponent.Scope scope = ConstraintComponent.Scope.nodeShape;

			if (this.propertyShapeScopeWithValue) {
				propertyShapeScopeWithValue = false;
				chain = chain.subList(0, chain.size() - 1);
			}

			return Collections.singletonList(new ValidationTuple(this.validationResults, chain, scope,
					propertyShapeScopeWithValue, Collections.emptySet()));

		} else {
			return this.compressedTuples.stream()
					.map(t -> {
						List<Value> chain = t.chain;

						boolean propertyShapeScopeWithValue = t.propertyShapeScopeWithValue;
						ConstraintComponent.Scope scope = ConstraintComponent.Scope.nodeShape;

						if (this.propertyShapeScopeWithValue) {
							propertyShapeScopeWithValue = false;
							chain = chain.subList(0, chain.size() - 1);
						}

						return new ValidationTuple(t.validationResults, chain, scope, propertyShapeScopeWithValue,
								Collections.emptySet());

					})
					.collect(Collectors.toList());

		}

	}

	public List<ValidationTuple> shiftToPropertyShapeScope() {
		assert scope == ConstraintComponent.Scope.nodeShape;
		assert chain.size() >= 2;

		boolean propertyShapeScopeWithValue = true;
		ConstraintComponent.Scope scope = ConstraintComponent.Scope.propertyShape;

		if (!compressedTuples.isEmpty()) {

			return compressedTuples.stream()
					.map(t -> new ValidationTuple(t.validationResults, t.chain, scope, propertyShapeScopeWithValue,
							Collections.emptySet()))
					.collect(Collectors.toList());

		} else {
			return Collections.singletonList(
					new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue,
							Collections.emptySet()));
		}

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
	public List<Value> getTargetChain(boolean includePropertyShapeValues) {

		if (scope == ConstraintComponent.Scope.propertyShape && hasValue() && !includePropertyShapeValues) {
			return chain.stream().limit(chain.size() - 1).collect(Collectors.toList());
		}

		return new ArrayList<>(chain);
	}

	public ValidationTuple setValue(Value value) {
		if (value.equals(getValue())) {
			return this;
		}

		List<Value> chain = new ArrayList<>(this.chain);

		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {
				chain.remove(this.chain.size() - 1);
			}
			chain.add(value);
		} else {
			throw new IllegalStateException(
					"Can't set value on NodeShape scoped ValidationTuple because it will also change the target!");

		}

		boolean propertyShapeScopeWithValue = true;
		Set<ValidationTuple> compressedTuples = this.compressedTuples.stream()
				.map(t -> t.setValue(value))
				.collect(Collectors.toSet());
		return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue, compressedTuples);
	}

	public int compareValue(ValidationTuple other) {
		Value left = getValue();
		Value right = other.getValue();

		return valueComparator.compare(left, right);
	}

	public ValidationTuple trimToTarget() {
		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {

				List<Value> chain = this.chain.subList(0, this.chain.size() - 1);
				boolean propertyShapeScopeWithValue = false;
				Set<ValidationTuple> compressedTuples = this.compressedTuples.stream()
						.map(ValidationTuple::trimToTarget)
						.collect(Collectors.toSet());

				return new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue,
						compressedTuples);
			}
		}
		return this;
	}

	public List<ValidationTuple> pop() {

		if (compressedTuples.isEmpty()) {
			List<Value> chain = this.chain;
			boolean propertyShapeScopeWithValue = this.propertyShapeScopeWithValue;
			if (getScope() == ConstraintComponent.Scope.propertyShape) {
				if (hasValue()) {
					assert chain.size() > 1 : "Attempting to pop chain will not leave any elements on the chain! "
							+ toString();
					chain = chain.subList(0, chain.size() - 1);
				} else {
					propertyShapeScopeWithValue = true;
				}
			} else {
				assert chain.size() > 1 : "Attempting to pop chain will not leave any elements on the chain! "
						+ toString();
				chain = chain.subList(0, chain.size() - 1);
			}

			return Collections.singletonList(
					new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue,
							Collections.emptySet()));
		} else {

			return compressedTuples.stream().flatMap(t1 -> {
				return t1.pop()
						.stream()
						.map(t -> new ValidationTuple(t.validationResults, t.chain, t.scope,
								t.propertyShapeScopeWithValue, t.compressedTuples));
			}).collect(Collectors.toList());

		}

	}

	public Set<ValidationTuple> getCompressedTuples() {
		return compressedTuples;
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
		return propertyShapeScopeWithValue == that.propertyShapeScopeWithValue && chain.equals(that.chain)
				&& scope == that.scope && validationResults.equals(that.validationResults)
				&& compressedTuples.equals(that.compressedTuples);
	}

	@Override
	public int hashCode() {
		return Objects.hash(chain, scope, propertyShapeScopeWithValue, validationResults, compressedTuples);
	}

	public ValidationTuple join(ValidationTuple right) {

		HashSet<ValidationTuple> compressedTuples = new HashSet<>(this.compressedTuples);
		compressedTuples.addAll(right.getCompressedTuples());

		ValidationTuple validationTuple = new ValidationTuple(validationResults, chain, scope,
				propertyShapeScopeWithValue, compressedTuples);
		if (scope == ConstraintComponent.Scope.propertyShape) {
			validationTuple = validationTuple.setValue(right.getValue());
		}
		return validationTuple;
	}
}
