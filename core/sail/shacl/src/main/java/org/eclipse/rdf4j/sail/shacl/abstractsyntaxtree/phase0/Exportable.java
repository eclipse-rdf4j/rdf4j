package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

public interface Exportable {

	void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported);
}
