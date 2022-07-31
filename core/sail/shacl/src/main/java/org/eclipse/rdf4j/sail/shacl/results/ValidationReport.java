/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results;

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

/**
 * The ValidationReport represents the report from a SHACL validation in an easy-to-use Java API.
 *
 * @deprecated The ValidationReport is deprecated because it is planned moved to a new package to allow it to be used
 *             with remote validation reports.
 */
@Deprecated
public class ValidationReport {

	protected Resource id = null;

	protected boolean conforms = true;

	protected final List<ValidationResult> validationResult = new ArrayList<>();
	protected boolean truncated = false;

	public ValidationReport() {

	}

	public ValidationReport(boolean conforms) {
		this.conforms = conforms;
	}

	public void addValidationResult(ValidationResult validationResult) {
		this.validationResult.add(validationResult);
	}

	public Model asModel(Model model) {

		model.add(getId(), SHACL.CONFORMS, literal(conforms));
		model.add(getId(), RDF.TYPE, SHACL.VALIDATION_REPORT);
		model.add(getId(), RDF4J.TRUNCATED, BooleanLiteral.valueOf(truncated));

		HashSet<Resource> rdfListDedupe = new HashSet<>();

		for (ValidationResult result : validationResult) {
			model.add(getId(), SHACL.RESULT, result.getId());
			result.asModel(model, rdfListDedupe);
		}

		return model;
	}

	public Model asModel() {
		return asModel(new DynamicModelFactory().createEmptyModel());
	}

	public final Resource getId() {
		if (id == null) {
			id = bnode();
		}
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

	@Override
	public String toString() {
		return "ValidationReport{" +
				"conforms=" + conforms +
				", validationResult=" + Arrays.toString(validationResult.toArray()) +
				'}';
	}

	/**
	 * Users can enable a limit for the number of validation results they want to accept. If the limit is reached the
	 * report will be marked as truncated.
	 *
	 * @return true if this SHACL validation report has been truncated.
	 */
	public boolean isTruncated() {
		return truncated;
	}

}
