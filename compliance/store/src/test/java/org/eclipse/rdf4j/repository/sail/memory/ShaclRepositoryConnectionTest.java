package org.eclipse.rdf4j.repository.sail.memory;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnectionTest;
import org.eclipse.rdf4j.repository.RepositoryTest;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

public class ShaclRepositoryConnectionTest extends RepositoryConnectionTest {

	public ShaclRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository() {
		SailRepository shaclShapes = new SailRepository(new MemoryStore());
		shaclShapes.initialize();
		return new SailRepository(new ShaclSail(new MemoryStore(), shaclShapes));
	}

}
