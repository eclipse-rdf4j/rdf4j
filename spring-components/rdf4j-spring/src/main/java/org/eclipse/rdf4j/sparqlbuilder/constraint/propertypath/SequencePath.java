package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

public class SequencePath implements PropertyPath {
	private final PropertyPath left;
	private final PropertyPath right;

	public SequencePath(PropertyPath left, PropertyPath right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String getQueryString() {
		return left.getQueryString() + " / " + right.getQueryString();
	}
}
