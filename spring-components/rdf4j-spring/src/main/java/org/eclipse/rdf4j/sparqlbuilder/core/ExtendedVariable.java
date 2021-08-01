package org.eclipse.rdf4j.sparqlbuilder.core;

public class ExtendedVariable extends Variable {
	private final String varName;

	public ExtendedVariable(String varName) {
		super(varName);
		this.varName = varName;
	}

	public String getVarName() {
		return varName;
	}
}
