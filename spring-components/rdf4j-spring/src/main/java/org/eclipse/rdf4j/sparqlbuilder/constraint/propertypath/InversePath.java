package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class InversePath implements PropertyPath {
	private final PropertyPath path;

	public InversePath(PropertyPath path) {
		this.path = path;
	}

	public String getQueryString() {
		return "^ " + path.getQueryString();
	}
}
