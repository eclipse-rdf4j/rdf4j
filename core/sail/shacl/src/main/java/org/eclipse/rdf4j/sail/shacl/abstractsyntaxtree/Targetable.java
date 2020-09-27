package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

import java.util.stream.Stream;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;

public interface Targetable {

	Stream<StatementPattern> getStatementPatterns(Var subject, Var object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner);

	String getTargetQueryFragment(Var subject, Var object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner);

}
