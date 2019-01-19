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

	@SuppressWarnings("WeakerAccess")
	public Model validationReportAsModel() {

		ValidationReport validationReport = getValidationReport();

		Model model = validationReport.asModel();
		model.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
		return model;


	}

	@SuppressWarnings("WeakerAccess")
	public ValidationReport getValidationReport() {
		ValidationReport validationReport = new ValidationReport(false);

		for (Tuple invalidTuple : invalidTuples) {
			ValidationResult parent = null;
			ArrayDeque<PropertyShape> propertyShapes = new ArrayDeque<>(invalidTuple.getCausedByPropertyShapes());

			while (!propertyShapes.isEmpty()) {
				ValidationResult validationResult = new ValidationResult(propertyShapes.pop(), (Resource) invalidTuple.line.get(0));
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
