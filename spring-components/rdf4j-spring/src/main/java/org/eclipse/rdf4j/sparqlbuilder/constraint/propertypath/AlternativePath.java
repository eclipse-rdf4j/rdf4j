package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class AlternativePath implements PropertyPath {
	private final PropertyPath left;
	private final PropertyPath right;

	public AlternativePath(PropertyPath left, PropertyPath right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String getQueryString() {
		return "( " + left.getQueryString() + " | " + right.getQueryString() + " )";
	}
}
