package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.model.Statement;

public interface WriteAheadLoggingInterface<T extends DataStructureInterface> {

	void init(T dataStructure);

	void begin();

	void commit();

	void statementToAdd(Statement statement);

	void statementToRemove(Statement statement);

}
