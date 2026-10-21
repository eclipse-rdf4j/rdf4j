/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SequentialPrefetchPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class ValidationQuery {

	private final Set<Namespace> namespaces = new HashSet<>();
	private ValidationResultGenerator validationResultGenerator;
	private String query;
	private ConstraintComponent.Scope scope;
	private ConstraintComponent.Scope scope_validationReport;

	private final List<Variable<Value>> variables;

	private int targetIndex;
	private int valueIndex;

	private boolean propertyShapeWithValue;
	private boolean propertyShapeWithValue_validationReport;

	private int targetIndex_validationReport;
	private int valueIndex_validationReport;

	private ConstraintComponent constraintComponent;
	private ConstraintComponent constraintComponent_validationReport;

	private Severity severity;
	private Shape shape;
	private List<Variable<?>> extraVariables = List.of();

	// NEW: if non-null, this ValidationQuery is a composite of independently
	// executable members, rather than a single leaf SPARQL query. When
	// non-null, `query`/`namespaces` on *this* object are not used to build
	// an executable query - execution is delegated to the members.
	private List<ValidationQuery> unionMembers;

	public ValidationQuery(Collection<Namespace> namespaces, String query, List<Variable<Value>> targets,
			Variable<Value> value,
			ConstraintComponent.Scope scope, ConstraintComponent constraintComponent, Severity severity,
			Shape shape) {

		this.namespaces.addAll(namespaces);
		this.query = query;

		var variables = new ArrayList<>(targets);
		if (value != null) {
			variables.add(value);
		}
		this.variables = Collections.unmodifiableList(variables);
		if (scope == ConstraintComponent.Scope.propertyShape) {
			targetIndex = targets.size() - 1;
			if (value != null) {
				propertyShapeWithValue = true;
				valueIndex = variables.size() - 1;
				assert constraintComponent == null
						|| constraintComponent.getConstraintComponent().producesValidationResultValue();
			} else {
				propertyShapeWithValue = false;
				valueIndex = variables.size();
				assert constraintComponent == null
						|| !constraintComponent.getConstraintComponent().alwaysProducesValidationResultValue();
			}
		} else {
			targetIndex = variables.size() - 1;
			valueIndex = variables.size() - 1;
		}

		this.scope = scope;
		this.constraintComponent = constraintComponent;
		this.severity = severity;
		this.shape = shape;
		this.validationResultGenerator = new ValidationResultGenerator();

	}

	public ValidationQuery(Set<Namespace> namespaces, String query, ConstraintComponent.Scope scope,
			List<Variable<Value>> variables,
			int targetIndex, int valueIndex) {
		this.namespaces.addAll(namespaces);
		this.query = query;
		this.scope = scope;
		this.variables = Collections.unmodifiableList(variables);
		this.targetIndex = targetIndex;
		this.valueIndex = valueIndex;
		this.validationResultGenerator = new ValidationResultGenerator();
	}

	// NEW: composite constructor. Shares the same "shape" metadata (variables,
	// indices, scope) that the old textual union() computed, but does not
	// carry an executable `query` string - execution happens per-member.
	private ValidationQuery(ConstraintComponent.Scope scope, List<Variable<Value>> variables,
			int targetIndex, int valueIndex, List<ValidationQuery> unionMembers) {
		this.query = null;
		this.scope = scope;
		this.variables = Collections.unmodifiableList(variables);
		this.targetIndex = targetIndex;
		this.valueIndex = valueIndex;
		this.unionMembers = unionMembers;
		this.validationResultGenerator = new ValidationResultGenerator();
	}

	/**
	 * Combines two ValidationQuery objects so that, at execution time, each is run as its own SPARQL query and the
	 * results are merged in Java, instead of being concatenated into a single SPARQL {@code UNION}.
	 * <p>
	 * This preserves the same set of result rows as the old textual-UNION approach, but avoids re-evaluating shared
	 * subpatterns (e.g. a large target-class filter) once per branch inside a single query plan.
	 *
	 * @param a              The first ValidationQuery.
	 * @param b              The second ValidationQuery.
	 * @param skipValueCheck Skips checks that the two ValidationQuery object are using the same value. This is useful
	 *                       if the ValidationQuery is guaranteed to not use the current value because
	 *                       {@link #shiftToNodeShape()} or {@link #popTargetChain()} will always called on the returned
	 *                       ValidationQuery
	 * @return
	 */
	public static ValidationQuery union(ValidationQuery a, ValidationQuery b, boolean skipValueCheck) {
		if (a == Deactivated.instance) {
			return b;
		}
		if (b == Deactivated.instance) {
			return a;
		}

		assert a.getTargetVariable(false).equals(b.getTargetVariable(false));
		assert skipValueCheck || !a.propertyShapeWithValue || !b.propertyShapeWithValue
				|| a.getValueVariable(false).equals(b.getValueVariable(false));
		assert a.scope != ConstraintComponent.Scope.nodeShape || a.valueIndex == a.targetIndex;
		assert b.scope != ConstraintComponent.Scope.nodeShape || b.valueIndex == b.targetIndex;
		assert a.scope == b.scope;
		assert a.targetIndex == b.targetIndex;
		assert a.valueIndex == b.valueIndex;

		// Flatten: if either side is already a composite, absorb its members
		// instead of nesting, so N-way folds (as in the .reduce(...) call
		// site) produce one flat list rather than a left-leaning tree.
		List<ValidationQuery> members = new ArrayList<>();
		if (a.unionMembers != null) {
			members.addAll(a.unionMembers);
		} else {
			members.add(a);
		}
		if (b.unionMembers != null) {
			members.addAll(b.unionMembers);
		} else {
			members.add(b);
		}

		var variables = a.variables.size() >= b.variables.size() ? a.variables
				: b.variables;

		if (a.propertyShapeWithValue || a.scope == ConstraintComponent.Scope.nodeShape) {
			assert a.variables.size() > a.valueIndex;
			return new ValidationQuery(a.scope, variables.subList(0, a.valueIndex + 1),
					a.targetIndex, a.valueIndex, members);
		} else {
			assert a.variables.size() >= a.valueIndex;
			return new ValidationQuery(a.scope, a.variables.subList(0, a.valueIndex),
					a.targetIndex, a.valueIndex, members);
		}
	}

	public String getQuery() {
		if (unionMembers != null) {
			// Debug/logging aid only - this is not one executable query anymore.
			return unionMembers.stream()
					.map(ValidationQuery::getQuery)
					.collect(Collectors.joining("\n-- (executed as a separate query, merged in Java) --\n"));
		}
		return query;
	}

	public PlanNode getValidationPlan(SailConnection baseConnection, Resource[] dataGraph,
			Resource[] shapesGraphs, boolean includeInferredStatements) {

		if (unionMembers != null) {
			SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(unionMembers.size());
			for (ValidationQuery m : unionMembers) {
				node.addDelegate(
						m.getValidationPlan(baseConnection, dataGraph, shapesGraphs, includeInferredStatements));
			}
			return node;
		}

		assert query != null;
		assert shape != null;
		assert scope_validationReport != null;

		String fullQueryString = getFullQueryString();

		Select select = new Select(baseConnection, fullQueryString, bindings -> {

			var validationResultFunction = validationResultGenerator.getValidationTupleValidationResultFunction(this,
					shapesGraphs, bindings);

			ValidationTuple validationTuple;

			if (scope_validationReport == ConstraintComponent.Scope.propertyShape) {
				if (propertyShapeWithValue_validationReport) {
					validationTuple = new ValidationTuple(bindings.getValue(getTargetVariable(true)),
							bindings.getValue(getValueVariable(true)),
							scope_validationReport, true, dataGraph);
				} else {
					validationTuple = new ValidationTuple(bindings.getValue(getTargetVariable(true)),
							scope_validationReport, false, dataGraph);
				}

			} else {
				validationTuple = new ValidationTuple(bindings.getValue(getTargetVariable(true)),
						scope_validationReport, true, dataGraph);
			}

			return validationTuple.addValidationResult(validationResultFunction);

		}, dataGraph, includeInferredStatements);

		return select;

	}

	public static class ValidationResultGenerator {

		public Function<ValidationTuple, ValidationResult> getValidationTupleValidationResultFunction(
				ValidationQuery validationQuery, Resource[] shapesGraphs, BindingSet bindings) {
			return t -> new ValidationResult(t.getActiveTarget(), t.getValue(), validationQuery.shape,
					validationQuery.constraintComponent_validationReport, validationQuery.severity, t.getScope(),
					t.getContexts(), shapesGraphs);
		}

	}

	public void setValidationResultGenerator(List<Variable<?>> extraVariables,
			ValidationResultGenerator validationResultGenerator) {
		if (unionMembers != null) {
			unionMembers.forEach(m -> m.setValidationResultGenerator(extraVariables, validationResultGenerator));
			return;
		}
		this.validationResultGenerator = validationResultGenerator;
		this.extraVariables = extraVariables;
	}

	private String getFullQueryString() {
		String extraVariablesString;
		if (!extraVariables.isEmpty()) {
			Optional<String> reduce = extraVariables.stream()
					.map(Variable::asSparqlVariable)
					.reduce((a, b) -> a + " " + b);
			if (reduce.isPresent()) {
				extraVariablesString = reduce.get() + " ";
			} else {
				extraVariablesString = "";
			}
		} else {
			extraVariablesString = "";
		}

		if (scope_validationReport == ConstraintComponent.Scope.propertyShape
				&& propertyShapeWithValue_validationReport) {

			return ShaclPrefixParser.toSparqlPrefixes(namespaces) + "\nSELECT DISTINCT " +
					"?" + getTargetVariable(true) + " " +
					"?" + getValueVariable(true) + " " +
					extraVariablesString +
					"WHERE {\n" + query + "\n}";

		} else {
			return ShaclPrefixParser.toSparqlPrefixes(namespaces) + "\nSELECT DISTINCT " +
					"?" + getTargetVariable(true) + " " +
					extraVariablesString +
					"WHERE {\n" + query + "\n}";
		}
	}

	private String getValueVariable(boolean forValidationReport) {
		if (forValidationReport) {
			return variables.get(valueIndex_validationReport).name;
		}
		assert scope != ConstraintComponent.Scope.propertyShape || propertyShapeWithValue;
		return variables.get(valueIndex).name;
	}

	private String getTargetVariable(boolean forValidationReport) {
		if (forValidationReport) {
			return variables.get(targetIndex_validationReport).name;
		}
		return variables.get(targetIndex).name;
	}

	public ValidationQuery withSeverity(Severity severity) {
		if (unionMembers != null) {
			unionMembers.forEach(m -> m.withSeverity(severity));
			return this;
		}
		this.severity = severity;
		return this;
	}

	public ValidationQuery withShape(Shape shape) {
		if (unionMembers != null) {
			unionMembers.forEach(m -> m.withShape(shape));
			return this;
		}
		this.shape = shape;
		return this;
	}

	public void popTargetChain() {
		assert scope == ConstraintComponent.Scope.propertyShape;
		this.propertyShapeWithValue = true;
		targetIndex--;
		valueIndex--;
		if (unionMembers != null) {
			unionMembers.forEach(ValidationQuery::popTargetChain);
		}
	}

	public void shiftToNodeShape() {
		assert this.scope == ConstraintComponent.Scope.propertyShape;
		this.scope = ConstraintComponent.Scope.nodeShape;
		this.propertyShapeWithValue = false;
		valueIndex--;
		if (unionMembers != null) {
			unionMembers.forEach(ValidationQuery::shiftToNodeShape);
		}
	}

	public void shiftToPropertyShape() {
		assert this.scope == ConstraintComponent.Scope.nodeShape;
		this.scope = ConstraintComponent.Scope.propertyShape;
		this.propertyShapeWithValue = true;
		targetIndex--;
		if (unionMembers != null) {
			unionMembers.forEach(ValidationQuery::shiftToPropertyShape);
		}
	}

	public ValidationQuery withConstraintComponent(ConstraintComponent constraintComponent) {
		if (unionMembers != null) {
			unionMembers.forEach(m -> m.withConstraintComponent(constraintComponent));
			return this;
		}
		this.constraintComponent = constraintComponent;
		return this;
	}

	public void makeCurrentStateValidationReport() {
		valueIndex_validationReport = valueIndex;
		targetIndex_validationReport = targetIndex;
		scope_validationReport = scope;
		constraintComponent_validationReport = constraintComponent;
		propertyShapeWithValue_validationReport = propertyShapeWithValue;
		if (unionMembers != null) {
			unionMembers.forEach(ValidationQuery::makeCurrentStateValidationReport);
		}
	}

	public Shape getShape() {
		return shape;
	}

	public Severity getSeverity() {
		return severity;
	}

	public ConstraintComponent getConstraintComponent_validationReport() {
		return constraintComponent_validationReport;
	}

	// used for sh:deactivated
	public static class Deactivated extends ValidationQuery {

		private static final Deactivated instance = new Deactivated();

		private Deactivated() {
			super(List.of(), "", Collections.emptyList(), null, null, null, null, null);
		}

		public static Deactivated getInstance() {
			return instance;
		}

		@Override
		public PlanNode getValidationPlan(SailConnection baseConnection, Resource[] dataGraph,
				Resource[] shapesGraphs, boolean includeInferredStatements) {
			return EmptyNode.getInstance();
		}

		@Override
		public ValidationQuery withSeverity(Severity severity) {
			return this;
		}

		@Override
		public ValidationQuery withShape(Shape shape) {
			return this;
		}

		@Override
		public void popTargetChain() {
			// no-op;
		}

		@Override
		public void shiftToNodeShape() {
			// no-op;
		}

		@Override
		public void shiftToPropertyShape() {
			// no-op;
		}

		@Override
		public ValidationQuery withConstraintComponent(ConstraintComponent constraintComponent) {
			return this;
		}

		@Override
		public void makeCurrentStateValidationReport() {
			// no-op;
		}
	}
}
