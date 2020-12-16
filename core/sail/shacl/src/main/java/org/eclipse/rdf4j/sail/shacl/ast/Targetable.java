package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.stream.Stream;

import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;

public interface Targetable {

	Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner);

	String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner);

}
