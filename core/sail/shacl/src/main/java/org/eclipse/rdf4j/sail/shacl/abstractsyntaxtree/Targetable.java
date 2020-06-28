package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

import java.util.stream.Stream;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

public interface Targetable {

	Stream<StatementPattern> getStatementPatterns(Var subject, Var object);

	String getQueryFragment(Var subject, Var object);

}
