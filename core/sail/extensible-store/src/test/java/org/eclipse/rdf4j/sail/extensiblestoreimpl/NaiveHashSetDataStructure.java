package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.common.iteration.IteratorIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;
import org.eclipse.rdf4j.sail.extensiblestore.FilteringIteration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NaiveHashSetDataStructure implements DataStructureInterface {

	Set<Statement> statements = new HashSet<>();

	@Override
	synchronized public void addStatement(Statement statement) {
		statements.add(statement);

	}

	@Override
	synchronized public void removeStatement(Statement statement) {
		statements.remove(statement);

	}

	@Override
	synchronized public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject,
			IRI predicate,
			Value object, Resource... context) {
		return new FilteringIteration<>(
				new IteratorIteration<Statement, SailException>(new ArrayList<>(statements).iterator()), subject,
				predicate, object, context);
	}

	@Override
	public void flushForReading() {

	}

	@Override
	public void init() {

	}

	@Override
	public void flushForCommit() {

	}
}
