package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;

class ComplexQueryFragment {
	private final String targetVarPrefix;
	private final StatementMatcher.Variable value;
	private final String query;
	private final StatementMatcher.Variable targetVar;

	public ComplexQueryFragment(String query, String targetVarPrefix, StatementMatcher.Variable targetVar,
			StatementMatcher.Variable value) {
		this.query = query;
		this.targetVarPrefix = targetVarPrefix;
		this.targetVar = targetVar;
		this.value = value;
	}

	public String getTargetVarPrefix() {
		return targetVarPrefix;
	}

	public StatementMatcher.Variable getValue() {
		return value;
	}

	public String getQuery() {
		return query;
	}

	public StatementMatcher.Variable getTargetVar() {
		return targetVar;
	}
}
