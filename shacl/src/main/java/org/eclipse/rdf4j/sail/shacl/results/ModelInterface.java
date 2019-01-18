package org.eclipse.rdf4j.sail.shacl.results;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;

public interface ModelInterface {

	default Model asModel(){
		return asModel(new LinkedHashModelFactory().createEmptyModel());
	}

	Model asModel(Model model);
	Resource getId();

}
