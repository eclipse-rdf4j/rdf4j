package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class ZeroOrOnePath implements PropertyPath {
	private final PropertyPath path;

	public ZeroOrOnePath(PropertyPath path) {
		this.path = path;
	}

	@Override
	public String getQueryString() {
		return path.getQueryString() + " ?";
	}
}
