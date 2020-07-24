/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.AST.InversePath;
import org.eclipse.rdf4j.sail.shacl.AST.Path;
import org.eclipse.rdf4j.sail.shacl.AST.PathPropertyShape;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.AST.SimplePath;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;

/**
 * The ValidationResult represents the results from a SHACL validation in an easy-to-use Java API.
 *
 * @deprecated The ValidationResult is deprecated because it is planned moved to a new package to allow it to be used
 *             with remote validation results.
 */
@Deprecated
public class ValidationResult {

	private final Resource id = SimpleValueFactory.getInstance().createBNode();

	private final SourceConstraintComponent sourceConstraintComponent;
	private final PropertyShape sourceShape;
	private Path path;
	private ValidationResult detail;
	private final Value focusNode;
	private final Optional<Value> value;

	static Set<SourceConstraintComponent.ConstraintType> constraintTypesThatSupportValue = Arrays
			.stream(SourceConstraintComponent.ConstraintType.values())
			.filter(t -> t != SourceConstraintComponent.ConstraintType.Cardinality)
			.filter(t -> t != SourceConstraintComponent.ConstraintType.Logical)
			.collect(Collectors.toSet());

	public ValidationResult(PropertyShape sourceShape, Value focusNode, Value value) {
		this.sourceShape = sourceShape;
		this.focusNode = focusNode;
		this.sourceConstraintComponent = sourceShape.getSourceConstraintComponent();
		if (sourceShape instanceof PathPropertyShape) {
			this.path = ((PathPropertyShape) sourceShape).getPath();
		}

		if (constraintTypesThatSupportValue.contains(sourceConstraintComponent.getConstraintType())) {
			this.value = Optional.of(value);
		} else {
			this.value = Optional.empty();
		}

	}

	public void setDetail(ValidationResult detail) {
		this.detail = detail;
	}

	/**
	 * @return ValidationResult with more information as to what failed. Usually for nested Shapes in eg. sh:or.
	 */
	public ValidationResult getDetail() {
		return detail;
	}

	/**
	 * @return all ValidationResult(s) with more information as to what failed. Usually for nested Shapes in eg. sh:or.
	 */
	public List<ValidationResult> getDetails() {

		ArrayList<ValidationResult> validationResults = new ArrayList<>();

		ValidationResult temp = detail;
		while (temp != null) {
			validationResults.add(temp);
			temp = temp.detail;
		}

		return validationResults;

	}

	public Model asModel(Model model) {

		model.add(getId(), RDF.TYPE, SHACL.VALIDATION_RESULT);

		model.add(getId(), SHACL.FOCUS_NODE, getFocusNode());
		model.add(getId(), SHACL.SOURCE_CONSTRAINT_COMPONENT, getSourceConstraintComponent().getIri());
		model.add(getId(), SHACL.SOURCE_SHAPE, getSourceShapeResource());

		if (getPath() != null) {
			// TODO: Path should be responsible for this!
			if (getPath() instanceof SimplePath) {
				model.add(getId(), SHACL.RESULT_PATH, ((SimplePath) getPath()).getPath());
			} else if (getPath() instanceof InversePath) {
				model.add(getId(), SHACL.RESULT_PATH, getPath().getId());
				model.add(getPath().getId(), SHACL.INVERSE_PATH, ((InversePath) getPath()).getPath());
			}
		}

		value.ifPresent(v -> model.add(getId(), SHACL.VALUE, v));

		if (detail != null) {
			model.add(getId(), SHACL.DETAIL, detail.getId());
			detail.asModel(model);
		}

		return model;
	}

	/**
	 * @return the path, as specified in the Shape, that caused the violation
	 */
	private Path getPath() {
		return path;
	}

	/**
	 * @return the Resource (IRI or BNode) that identifies the source shape
	 */
	public Resource getSourceShapeResource() {
		return sourceShape.getId();
	}

	/**
	 * @return the focus node, aka. the subject, that caused the violation
	 */
	private Value getFocusNode() {
		return focusNode;
	}

	public Resource getId() {
		return id;
	}

	/**
	 * @return the type of the source constraint that caused the violation
	 */
	public SourceConstraintComponent getSourceConstraintComponent() {
		return sourceConstraintComponent;
	}

	@Override
	public String toString() {
		return "ValidationResult{" +
				"sourceConstraintComponent=" + sourceConstraintComponent +
				", sourceShape=" + sourceShape +
				", path=" + path +
				", detail=" + detail +
				", focusNode=" + focusNode +
				'}';
	}

	public Optional<Value> getValue() {
		return value;
	}
}
