package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class ZeroOrMorePath implements PropertyPath {
	private final PropertyPath path;

	public ZeroOrMorePath(PropertyPath path) {
		this.path = path;
	}

	@Override
	public String getQueryString() {
		return path.getQueryString() + " *";
	}
}
