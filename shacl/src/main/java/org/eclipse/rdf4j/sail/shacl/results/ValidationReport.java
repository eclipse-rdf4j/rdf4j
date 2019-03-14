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

import java.util.ArrayList;
import java.util.List;

public class ValidationReport implements ModelInterface {

	private Resource id = SimpleValueFactory.getInstance().createBNode();

	private boolean conforms;

	private List<ValidationResult> validationResult = new ArrayList<>();

	public ValidationReport(boolean conforms) {
		this.conforms = conforms;
	}

	public void addValidationResult(ValidationResult validationResult) {
		this.validationResult.add(validationResult);
	}

	@Override
	public Model asModel(Model model) {

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		model.add(getId(), SHACL.CONFORMS, vf.createLiteral(conforms));
		model.add(getId(), RDF.TYPE, SHACL.VALIDATION_REPORT);

		for (ValidationResult result : validationResult) {
			model.add(getId(), SHACL.RESULT, result.getId());
			result.asModel(model);
		}

		return model;
	}

	@Override
	public Resource getId() {
		return id;
	}

	/**
	 * @return false if the changes violated a SHACL Shape
	 */
	public boolean conforms() {
		return conforms;
	}

	/**
	 * @return list of ValidationResult with more information about each violation
	 */
	public List<ValidationResult> getValidationResult() {
		return validationResult;
	}
}
