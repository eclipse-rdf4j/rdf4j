package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.query.algebra.Var;

class ComplexQueryFragment {
	private final String targetVarPrefix;
	private final Var value;
	private final String query;
	private final Var targetVar;

	public ComplexQueryFragment(String query, String targetVarPrefix, Var targetVar, Var value) {
		this.query = query;
		this.targetVarPrefix = targetVarPrefix;
		this.targetVar = targetVar;
		this.value = value;
	}

	public String getTargetVarPrefix() {
		return targetVarPrefix;
	}

	public Var getValue() {
		return value;
	}

	public String getQuery() {
		return query;
	}

	public Var getTargetVar() {
		return targetVar;
	}
}
