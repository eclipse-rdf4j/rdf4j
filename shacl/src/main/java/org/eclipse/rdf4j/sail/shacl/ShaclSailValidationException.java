/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

import java.util.ArrayDeque;
import java.util.List;

public class ShaclSailValidationException extends SailException {

	private List<Tuple> invalidTuples;

	ShaclSailValidationException(List<Tuple> invalidTuples) {
		super("Failed SHACL validation");
		this.invalidTuples = invalidTuples;
	}

	/**
	 * @return A Model containing the validation report as specified by the SHACL Recommendation
	 */
	@SuppressWarnings("WeakerAccess")
	public Model validationReportAsModel() {

		ValidationReport validationReport = getValidationReport();

		Model model = validationReport.asModel();
		model.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
		return model;

	}

	/**
	 * @return A ValidationReport Java object that describes what failed and can optionally be converted to a Model as
	 *         specified by the SHACL Recommendation
	 */
	@SuppressWarnings("WeakerAccess")
	public ValidationReport getValidationReport() {
		ValidationReport validationReport = new ValidationReport(false);

		for (Tuple invalidTuple : invalidTuples) {
			ValidationResult parent = null;
			ArrayDeque<PropertyShape> propertyShapes = new ArrayDeque<>(invalidTuple.getCausedByPropertyShapes());

			while (!propertyShapes.isEmpty()) {
				ValidationResult validationResult = new ValidationResult(propertyShapes.pop(),
						(Resource) invalidTuple.line.get(0));
				if (parent == null) {
					validationReport.addValidationResult(validationResult);
				} else {
					parent.setDetail(validationResult);
				}
				parent = validationResult;
			}

		}
		return validationReport;
	}
}
