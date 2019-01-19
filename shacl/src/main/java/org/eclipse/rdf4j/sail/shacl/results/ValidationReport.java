package org.eclipse.rdf4j.sail.shacl.results;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.ArrayList;
import java.util.List;

public class ValidationReport implements  ModelInterface{

	private Resource id = SimpleValueFactory.getInstance().createBNode();

	private boolean conforms;

	private List<ValidationResult> validationResult = new ArrayList<>();


	public ValidationReport(boolean conforms) {
		this.conforms = conforms;
	}

	public void addValidationResult(ValidationResult validationResult){
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
}
