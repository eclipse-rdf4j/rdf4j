package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Exportable;

import java.util.Collection;
import java.util.List;

public abstract class Target implements Exportable {

	public abstract IRI getPredicate();

}
