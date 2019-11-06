package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

public class DummyDataStruct extends DataStructureInterface {
	@Override
	public void addStatement(Statement statement) {

		System.out.println("HERE");
	}

	@Override
	public void removeStatement(Statement statement) {

	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject, IRI predicate,
			Value object, Resource... context) {
		return null;
	}

	@Override
	public void flush() {

	}
}
