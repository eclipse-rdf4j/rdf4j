package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConcurrencyTest;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.Ignore;
import org.junit.Test;

public class ShaclConcurrencyTest extends SailConcurrencyTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail()
		throws SailException
	{
		SailRepository shaclShapes = new SailRepository(new MemoryStore());
		shaclShapes.initialize();
		return new ShaclSail(new MemoryStore(), shaclShapes);
	}

	@Ignore
	@Test
	@Override
	public void testConcurrentAddLargeTxnRollback()
		throws Exception
	{
		// empty since this test is ignored
	}
}
