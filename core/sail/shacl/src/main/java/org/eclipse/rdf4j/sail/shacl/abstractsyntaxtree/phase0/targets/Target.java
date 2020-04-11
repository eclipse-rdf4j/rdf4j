package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Exportable;

public abstract class Target implements Exportable {

	public abstract IRI getPredicate();

}
