package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailIsolationLevelTest;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

public class ShaclIsolationLevelTest extends SailIsolationLevelTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail()
		throws SailException {
		SailRepository shaclShapes = new SailRepository(new MemoryStore());
		shaclShapes.initialize();
		return new ShaclSail(new MemoryStore(), shaclShapes);
	}
}