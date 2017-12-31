package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.repository.Repository;

public interface RequiresEvalutation {

	boolean requiresEvalutation(Repository addedStatements, Repository removedStatements);

}
