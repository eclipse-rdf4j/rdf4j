package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class GroupedPath implements PropertyPath {
	private final PropertyPath path;

	public GroupedPath(PropertyPath path) {
		this.path = path;
	}

	@Override
	public String getQueryString() {
		return "( " + path.getQueryString() + " )";
	}
}
