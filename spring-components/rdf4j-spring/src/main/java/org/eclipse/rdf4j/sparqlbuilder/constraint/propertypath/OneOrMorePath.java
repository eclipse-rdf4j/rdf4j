package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class OneOrMorePath implements PropertyPath {
	private final PropertyPath path;

	public OneOrMorePath(PropertyPath path) {
		this.path = path;
	}

	@Override
	public String getQueryString() {
		return path.getQueryString() + " +";
	}
}
