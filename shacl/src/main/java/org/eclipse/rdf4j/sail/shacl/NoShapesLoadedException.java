package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF4J;

public class NoShapesLoadedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoShapesLoadedException() {
		super("Load shapes by adding them to named graph <" + RDF4J.SHACL_SHAPE_GRAPH
				+ "> in the first transaction after initialization!");
	}
}
