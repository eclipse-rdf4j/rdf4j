/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Resource;
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
	private final Value[] chain;
	private final ConstraintComponent.Scope scope;
	private final boolean propertyShapeScopeWithValue;
	private final List<ValidationResult> validationResults;
	private final Set<ValidationTuple> compressedTuples;

	private final Resource[] contexts;

	public ValidationTuple(BindingSet bindingSet, String[] variables, ConstraintComponent.Scope scope,
			boolean hasValue, Resource[] contexts) {
		this(bindingSet, Arrays.asList(variables), scope, hasValue, contexts);
	}

	public ValidationTuple(BindingSet bindingSet, List<String> variables, ConstraintComponent.Scope scope,
			boolean hasValue, Resource[] contexts) {

		chain = new Value[variables.size()];

		for (int i = 0; i < variables.size(); i++) {
			chain[i] = bindingSet.getValue(variables.get(i));
		}

		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = List.of();
		this.compressedTuples = Set.of();
		this.contexts = contexts;
	}

	public ValidationTuple(List<Value> chain, ConstraintComponent.Scope scope, boolean hasValue, Resource[] contexts) {
		this.chain = chain.toArray(new Value[0]);
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = List.of();
		this.compressedTuples = Set.of();
		this.contexts = contexts;
	}

	// We will assume that the provided chain will not be mutated elsewhere.
	public ValidationTuple(Value[] chain, ConstraintComponent.Scope scope, boolean hasValue, Resource[] contexts) {
		this.chain = chain;
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = List.of();
		this.compressedTuples = Set.of();
		this.contexts = contexts;
	}

	public ValidationTuple(Value a, Value c, ConstraintComponent.Scope scope, boolean hasValue, Resource context) {
		this(a, c, scope, hasValue, new Resource[] { context });
	}

	public ValidationTuple(Value a, Value c, ConstraintComponent.Scope scope, boolean hasValue, Resource[] contexts) {
		chain = new Value[] { a, c };

		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = List.of();
		this.compressedTuples = Set.of();
		this.contexts = contexts;
	}

	public ValidationTuple(Value subject, ConstraintComponent.Scope scope, boolean hasValue, Resource context) {
		this(subject, scope, hasValue, new Resource[] { context });
	}

	public ValidationTuple(Value subject, ConstraintComponent.Scope scope, boolean hasValue, Resource[] contexts) {
		chain = new Value[] { subject };
		this.scope = scope;
		this.propertyShapeScopeWithValue = hasValue;
		this.validationResults = List.of();
		this.compressedTuples = Set.of();
		this.contexts = contexts;
	}

	private ValidationTuple(List<ValidationResult> validationResults, Value[] chain,
			ConstraintComponent.Scope scope, boolean propertyShapeScopeWithValue,
			Set<ValidationTuple> compressedTuples, Resource[] contexts) {
		this.validationResults = Collections.unmodifiableList(validationResults);
		this.chain = chain;
		this.scope = scope;
		this.propertyShapeScopeWithValue = propertyShapeScopeWithValue;
		this.compressedTuples = compressedTuples.isEmpty() ? Set.of() : Collections.unmodifiableSet(compressedTuples);
		this.contexts = contexts;

	}

	public ValidationTuple(ValidationTuple tuple, Set<ValidationTuple> compressedTuples) {
		this.validationResults = tuple.validationResults;
		this.chain = tuple.chain;
		this.scope = tuple.scope;
		this.propertyShapeScopeWithValue = tuple.propertyShapeScopeWithValue;
		this.compressedTuples = compressedTuples.isEmpty() ? Set.of() : Collections.unmodifiableSet(compressedTuples);
		this.contexts = tuple.contexts;
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
			return chain[(chain.length - 1)];
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

		List<Value> targetChain = getTargetChain(false);
		List<Value> otherTargetChain = other.getTargetChain(false);

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

		Set<ValidationTuple> compressedTuples = enrichCompressedTuples(t -> t.addValidationResult(validationResult));

		return new ValidationTuple(validationResults, chain, scope, propertyShapeScopeWithValue, compressedTuples,
				contexts);
	}

	public Value getActiveTarget() {
		assert scope != null;
		if (!propertyShapeScopeWithValue || scope != ConstraintComponent.Scope.propertyShape) {
			return chain[chain.length - 1];
		}

		assert chain.length >= 2;

		return chain[chain.length - 2];

	}

	@Override
	public String toString() {
		return "ValidationTuple{" +
				"chain=" + Arrays.toString(chain) +
				", scope=" + scope +
				", propertyShapeScopeWithValue=" + propertyShapeScopeWithValue +
//			", validationResults=" + validationResults +
				", compressedTuples=" + Arrays.toString(compressedTuples.toArray()) +
				'}';
	}

	public List<ValidationTuple> shiftToNodeShape() {
		assert scope == ConstraintComponent.Scope.propertyShape;

		if (compressedTuples.isEmpty()) {
			Value[] chain;
			boolean propertyShapeScopeWithValue = this.propertyShapeScopeWithValue;
			ConstraintComponent.Scope scope = ConstraintComponent.Scope.nodeShape;

			if (this.propertyShapeScopeWithValue) {
				propertyShapeScopeWithValue = false;
				chain = Arrays.copyOf(this.chain, this.chain.length - 1);
			} else {
				chain = this.chain;
			}

			return Collections
					.singletonList(new ValidationTuple(this.validationResults, chain, scope,
							propertyShapeScopeWithValue, Set.of(), contexts));

		} else {
			return this.compressedTuples.stream()
					.map(t -> {
						List<Value> chain = Arrays.asList(t.chain);

						boolean propertyShapeScopeWithValue = t.propertyShapeScopeWithValue;
						ConstraintComponent.Scope scope = ConstraintComponent.Scope.nodeShape;

						if (this.propertyShapeScopeWithValue) {
							propertyShapeScopeWithValue = false;
							chain = chain.subList(0, chain.size() - 1);
						}

						return new ValidationTuple(t.validationResults, chain.toArray(new Value[0]), scope,
								propertyShapeScopeWithValue,
								Set.of(), t.contexts);

					})
					.collect(Collectors.toList());

		}

	}

	public List<ValidationTuple> shiftToPropertyShapeScope() {
		assert scope == ConstraintComponent.Scope.nodeShape;
		assert chain.length >= 2;

		boolean propertyShapeScopeWithValue = true;
		ConstraintComponent.Scope scope = ConstraintComponent.Scope.propertyShape;

		if (!compressedTuples.isEmpty()) {

			return compressedTuples.stream()
					.map(t -> new ValidationTuple(t.validationResults, t.chain, scope, propertyShapeScopeWithValue,
							Set.of(), t.contexts))
					.collect(Collectors.toList());

		} else {
			return Collections.singletonList(
					new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue,
							Set.of(), contexts));
		}

	}

	public int getFullChainSize(boolean includePropertyShapeValue) {
		if (!includePropertyShapeValue && propertyShapeScopeWithValue) {
			return chain.length - 1;
		}

		return chain.length;

	}

	/**
	 * This is only the target part. For property shape scope it will not include the value.
	 *
	 * @param includePropertyShapeValues
	 */
	public List<Value> getTargetChain(boolean includePropertyShapeValues) {

		if (scope == ConstraintComponent.Scope.propertyShape && hasValue() && !includePropertyShapeValues) {
			return Collections.unmodifiableList(Arrays.asList(chain).subList(0, chain.length - 1));
		}

		return Collections.unmodifiableList(Arrays.asList(chain));
	}

	public ValidationTuple setValue(Value value) {
		if (value.equals(getValue())) {
			return this;
		}

		assert scope == ConstraintComponent.Scope.propertyShape : "Can't set value on NodeShape scoped ValidationTuple because it will also change the target!";

		Value[] chain;

		if (propertyShapeScopeWithValue) {
			// we will replace the last value, so we just need a copy because the chain should be immutable
			chain = Arrays.copyOf(this.chain, this.chain.length);
		} else {
			chain = Arrays.copyOf(this.chain, this.chain.length + 1);
		}

		chain[chain.length - 1] = value;

		Set<ValidationTuple> compressedTuples = enrichCompressedTuples(t -> t.setValue(value));

		return new ValidationTuple(this.validationResults, chain, scope, true, compressedTuples, contexts);
	}

	private Set<ValidationTuple> enrichCompressedTuples(
			Function<ValidationTuple, ValidationTuple> validationTupleValidationTupleFunction) {
		if (compressedTuples.isEmpty()) {
			return compressedTuples;
		}

		return this.compressedTuples.stream()
				.map(validationTupleValidationTupleFunction)
				.collect(Collectors.toSet());

	}

	public int compareValue(ValidationTuple other) {
		Value left = getValue();
		Value right = other.getValue();

		return valueComparator.compare(left, right);
	}

	public ValidationTuple trimToTarget() {
		if (scope == ConstraintComponent.Scope.propertyShape) {
			if (propertyShapeScopeWithValue) {

				Value[] chain = Arrays.copyOf(this.chain, this.chain.length - 1);

				Set<ValidationTuple> compressedTuples = enrichCompressedTuples(ValidationTuple::trimToTarget);

				return new ValidationTuple(validationResults, chain, scope, false, compressedTuples, contexts);
			}
		}
		return this;
	}

	public List<ValidationTuple> pop() {

		if (compressedTuples.isEmpty()) {

			Value[] chain;

			boolean propertyShapeScopeWithValue = this.propertyShapeScopeWithValue;
			if (getScope() == ConstraintComponent.Scope.propertyShape) {
				if (hasValue()) {
					assert this.chain.length > 1 : "Attempting to pop chain will not leave any elements on the chain! "
							+ this;
					chain = Arrays.copyOf(this.chain, this.chain.length - 1);
				} else {
					propertyShapeScopeWithValue = true;
					chain = this.chain;
				}
			} else {
				assert this.chain.length > 1 : "Attempting to pop chain will not leave any elements on the chain! "
						+ this;
				chain = Arrays.copyOf(this.chain, this.chain.length - 1);
			}

			return Collections.singletonList(
					new ValidationTuple(this.validationResults, chain, scope, propertyShapeScopeWithValue,
							Set.of(), contexts));
		} else {

			return compressedTuples.stream().flatMap(t1 -> {
				return t1.pop()
						.stream()
						.map(t -> new ValidationTuple(t.validationResults, t.chain, t.scope,
								t.propertyShapeScopeWithValue, t.compressedTuples, t.contexts));
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
		return propertyShapeScopeWithValue == that.propertyShapeScopeWithValue && Arrays.equals(chain, that.chain)
				&& scope == that.scope && validationResults.equals(that.validationResults)
				&& compressedTuples.equals(that.compressedTuples);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(chain), scope, propertyShapeScopeWithValue, validationResults,
				compressedTuples);
	}

	public ValidationTuple join(ValidationTuple right) {

		Set<ValidationTuple> compressedTuples;
		if (this.compressedTuples.isEmpty()) {
			compressedTuples = right.getCompressedTuples();
		} else if (right.compressedTuples.isEmpty()) {
			compressedTuples = this.compressedTuples;
		} else {
			compressedTuples = new HashSet<>(this.compressedTuples);
			compressedTuples.addAll(right.getCompressedTuples());
		}

		Resource[] contexts;

		if (this.contexts != right.contexts) {
			assert this.contexts != null;
			assert right.contexts != null;
			if (this.contexts.length == 1 && right.contexts.length == 1
					&& this.contexts[0] == right.contexts[0]) {
				contexts = this.contexts;
			} else if (this.contexts.length > 0 && right.contexts.length > 0) {
				contexts = Arrays.copyOf(this.contexts, this.contexts.length + right.contexts.length);
				System.arraycopy(right.contexts, 0, contexts, this.contexts.length, right.contexts.length);
			} else if (right.contexts.length > 0) {
				// this.contexts must be an empty array
				contexts = right.contexts;
			} else {
				contexts = this.contexts;
			}
		} else {
			contexts = this.contexts;
		}

		ValidationTuple validationTuple = new ValidationTuple(validationResults, chain, scope,
				propertyShapeScopeWithValue, compressedTuples, contexts);
		if (scope == ConstraintComponent.Scope.propertyShape) {
			validationTuple = validationTuple.setValue(right.getValue());
		}
		return validationTuple;
	}

	public Resource[] getContexts() {
		return contexts;
	}
}
