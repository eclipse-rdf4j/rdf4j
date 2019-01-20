/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.AST.Path;
import org.eclipse.rdf4j.sail.shacl.AST.PathPropertyShape;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.AST.SimplePath;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult implements ModelInterface {

	private Resource id = SimpleValueFactory.getInstance().createBNode();

	private SourceConstraintComponent sourceConstraintComponent;
	private PropertyShape sourceShape;
	private Path path;
	private ValidationResult detail;
	private Resource focusNode;

	public ValidationResult(PropertyShape sourceShape, Resource focusNode) {
		this.sourceShape = sourceShape;
		this.focusNode = focusNode;
		this.sourceConstraintComponent = sourceShape.getSourceConstraintComponent();
		if (sourceShape instanceof PathPropertyShape) {
			this.path = ((PathPropertyShape) sourceShape).getPath();
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
	public List<ValidationResult> getDetails(){

		ArrayList<ValidationResult> validationResults = new ArrayList<>();

		ValidationResult temp = detail;
		while(temp != null){
			validationResults.add(temp);
			temp = temp.detail;
		}

		return validationResults;

	}

	@Override
	public Model asModel(Model model) {

		model.add(getId(), RDF.TYPE, SHACL.VALIDATION_RESULT);

		model.add(getId(), SHACL.FOCUS_NODE, getFocusNode());
		model.add(getId(), SHACL.SOURCE_CONSTRAINT_COMPONENT, getSourceConstraintComponent().getIri());
		model.add(getId(), SHACL.SOURCE_SHAPE, getSourceShapeResource());

		if (getPath() != null) {
			model.add(getId(), SHACL.RESULT_PATH, ((SimplePath) getPath()).getPath());
		}

		if(detail != null){
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
	private Resource getFocusNode() {
		return focusNode;
	}

	@Override
	public Resource getId() {
		return id;
	}


	/**
	 * @return the type of the source constraint that caused the violation
	 */
	public SourceConstraintComponent getSourceConstraintComponent() {
		return sourceConstraintComponent;
	}
}
