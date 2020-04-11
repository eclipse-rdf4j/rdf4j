package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

import java.util.Set;

public interface Exportable {

	void toModel(Resource subject, Model model, Set<Resource> exported);
}
