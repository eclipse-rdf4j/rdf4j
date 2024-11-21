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

	/**
	 * Creates the SPARQL UNION of two ValidationQuery objects.
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

		String unionQuery = "{\n" + a.getQuery() + "\n} UNION {\n" + b.query + "\n}";

		var variables = a.variables.size() >= b.variables.size() ? a.variables
				: b.variables;

		Set<Namespace> namespaces = new HashSet<>();
		namespaces.addAll(a.namespaces);
		namespaces.addAll(b.namespaces);

		if (a.propertyShapeWithValue || a.scope == ConstraintComponent.Scope.nodeShape) {
			assert a.variables.size() > a.valueIndex;
			return new ValidationQuery(namespaces, unionQuery, a.scope, variables.subList(0, a.valueIndex + 1),
					a.targetIndex,
					a.valueIndex);
		} else {
			assert a.variables.size() >= a.valueIndex;
			return new ValidationQuery(namespaces, unionQuery, a.scope, a.variables.subList(0, a.valueIndex),
					a.targetIndex,
					a.valueIndex);
		}

	}

	public String getQuery() {
		return query;
	}

	public PlanNode getValidationPlan(SailConnection baseConnection, Resource[] dataGraph,
			Resource[] shapesGraphs) {

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

		}, dataGraph);

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
		this.severity = severity;
		return this;
	}

	public ValidationQuery withShape(Shape shape) {
		this.shape = shape;
		return this;
	}

	public void popTargetChain() {
		assert scope == ConstraintComponent.Scope.propertyShape;
		this.propertyShapeWithValue = true;
		targetIndex--;
		valueIndex--;
	}

	public void shiftToNodeShape() {
		assert this.scope == ConstraintComponent.Scope.propertyShape;
		this.scope = ConstraintComponent.Scope.nodeShape;
		this.propertyShapeWithValue = false;
		valueIndex--;
	}

	public void shiftToPropertyShape() {
		assert this.scope == ConstraintComponent.Scope.nodeShape;
		this.scope = ConstraintComponent.Scope.propertyShape;
		this.propertyShapeWithValue = true;
		targetIndex--;
	}

	public ValidationQuery withConstraintComponent(ConstraintComponent constraintComponent) {
		this.constraintComponent = constraintComponent;
		return this;
	}

	public void makeCurrentStateValidationReport() {
		valueIndex_validationReport = valueIndex;
		targetIndex_validationReport = targetIndex;
		scope_validationReport = scope;
		constraintComponent_validationReport = constraintComponent;
		propertyShapeWithValue_validationReport = propertyShapeWithValue;
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
				Resource[] shapesGraphs) {
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
