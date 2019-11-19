package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class DataStructure implements DataStructureInterface {

	private SailRepository memoryStore = new SailRepository(new MemoryStore());

	@Override
	public void addStatement(Statement statement) {
		try (SailRepositoryConnection connection = memoryStore.getConnection()) {
			connection.add(statement);
		}

	}

	@Override
	public void removeStatement(Statement statement) {
		try (SailRepositoryConnection connection = memoryStore.getConnection()) {
			connection.remove(statement);
		}
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject, IRI predicate,
			Value object, Resource... context) {

		return new CloseableIteration<Statement, SailException>() {

			SailRepositoryConnection connection = memoryStore.getConnection();
			RepositoryResult<Statement> statements = connection.getStatements(subject, predicate, object, context);

			@Override
			public boolean hasNext() throws SailException {
				return statements.hasNext();
			}

			@Override
			public Statement next() throws SailException {
				return statements.next();
			}

			@Override
			public void remove() throws SailException {

			}

			@Override
			public void close() throws SailException {

				statements.close();
				connection.close();
			}
		};

	}

	@Override
	public void flush() {

	}

	@Override
	public void init() {

	}

	@Override
	public void clear(Resource[] contexts) {
		try (SailRepositoryConnection connection = memoryStore.getConnection()) {
			connection.clear(contexts);
		}
	}

	@Override
	public void flushThrough() {

	}
}
