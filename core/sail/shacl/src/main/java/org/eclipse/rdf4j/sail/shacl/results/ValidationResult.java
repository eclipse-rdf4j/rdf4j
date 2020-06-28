/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Severity;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths.Path;

/**
 * The ValidationResult represents the results from a SHACL validation in an easy-to-use Java API.
 *
 * @deprecated The ValidationResult is deprecated because it is planned moved to a new package to allow it to be used
 *             with remote validation results.
 */
@Deprecated
public class ValidationResult {

	private final Resource id = SimpleValueFactory.getInstance().createBNode(UUID.randomUUID() + "");
	private final Value value;
	private final Shape shape;

	private final SourceConstraintComponent sourceConstraintComponent;
	private final Severity severity;
	private final Value focusNode;
	private Path path;
	private ValidationResult detail;

	public ValidationResult(Value focusNode, Value anyValue, Shape shape,
			SourceConstraintComponent sourceConstraintComponent, Severity severity) {
		this.focusNode = focusNode;
		this.sourceConstraintComponent = sourceConstraintComponent;
		this.value = anyValue;
		this.shape = shape;
		if (shape instanceof PropertyShape) {
			this.path = ((PropertyShape) shape).getPath();
		}
		this.severity = severity;
	}

	/**
	 * @return ValidationResult with more information as to what failed. Usually for nested Shapes in eg. sh:or.
	 */
	public ValidationResult getDetail() {
		return detail;
	}

	public void setDetail(ValidationResult detail) {
		this.detail = detail;
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

		model.add(getId(), SHACL.FOCUS_NODE, focusNode);
		model.add(getId(), SHACL.VALUE, value);

		if (this.path != null) {
			path.toModel(path.getId(), null, model, new HashSet<>());
			model.add(getId(), SHACL.RESULT_PATH, path.getId());
		}

		model.add(getId(), SHACL.SOURCE_CONSTRAINT_COMPONENT, getSourceConstraintComponent().getIri());
		model.add(getId(), SHACL.RESULT_SEVERITY, severity.getIri());

		if (detail != null) {
			model.add(getId(), SHACL.DETAIL, detail.getId());
			detail.asModel(model);
		}

		shape.toModel(getId(), SHACL.SOURCE_SHAPE, model, new HashSet<>());

		return model;
	}

	/**
	 * @return the path, as specified in the Shape, that caused the violation
	 */
	private Path getPath() {
		return path;
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
				", path=" + path +
				", detail=" + detail +
				", focusNode=" + focusNode +
				'}';
	}
}
