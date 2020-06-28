package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

import java.util.Set;

public interface Exportable {

	void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported);
}
