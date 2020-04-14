package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class DatatypeConstraintComponent implements ConstraintComponent {

	Resource datatype;

	public DatatypeConstraintComponent(ConstraintComponent parent, Resource datatype) {
		this.datatype = datatype;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.DATATYPE, datatype);
	}
}
