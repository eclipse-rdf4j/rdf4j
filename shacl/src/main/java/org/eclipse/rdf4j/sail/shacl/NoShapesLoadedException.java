package org.eclipse.rdf4j.sail.shacl;

public class NoShapesLoadedException extends RuntimeException {

	public NoShapesLoadedException() {
		super("Load shapes by adding them to named graph <"+ShaclSail.SHAPE_GRAPH+"> in the first transaction after initialization!");
	}
}
