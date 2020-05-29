/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.ArrayDeque;
import java.util.List;

import org.eclipse.rdf4j.exceptions.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class ShaclSailValidationException extends SailException implements ValidationException {

	private List<Tuple> invalidTuples;

	ShaclSailValidationException(List<Tuple> invalidTuples) {
		super("Failed SHACL validation");
		this.invalidTuples = invalidTuples;
	}

	/**
	 * @return A Model containing the validation report as specified by the SHACL Recommendation
	 */
	@Override
	public Model validationReportAsModel() {

		ValidationReport validationReport = getValidationReport();

		Model model = validationReport.asModel();
		model.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
		return model;

	}

	/**
	 * @deprecated The returned ValidationReport is planned to be moved to a different package and this method is
	 *             planned to return that class.
	 *
	 * @return A ValidationReport Java object that describes what failed and can optionally be converted to a Model as
	 *         specified by the SHACL Recommendation
	 */
	@Deprecated
	public ValidationReport getValidationReport() {
		ValidationReport validationReport = new ValidationReport(invalidTuples.isEmpty());

		for (Tuple invalidTuple : invalidTuples) {
			ValidationResult parent = null;
			ArrayDeque<PropertyShape> propertyShapes = new ArrayDeque<>(invalidTuple.getCausedByPropertyShapes());

			while (!propertyShapes.isEmpty()) {
				ValidationResult validationResult = new ValidationResult(propertyShapes.pop(),
						invalidTuple.line.get(0));
				if (invalidTuple.line.size() > 1) {
					validationResult.SetAct(invalidTuple.line.get(1));
				}
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
