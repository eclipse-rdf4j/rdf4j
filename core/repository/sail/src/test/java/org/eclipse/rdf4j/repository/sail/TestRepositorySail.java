package org.eclipse.rdf4j.repository.sail;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestRepositorySail {

	private Sail sail;

	private final Sail memory = new MemoryStore();

	private final Repository repository = new SailRepository(memory);

	@Rule
	public final TemporaryFolder dataDir = new TemporaryFolder();

	@Before
	public final void setUp()
		throws RepositoryConfigException, RepositoryException
	{
		sail = new RepositorySail(repository);
		sail.initialize();
	}

	@After
	public final void tearDown()
		throws RepositoryException
	{
		sail.shutDown();
	}

	@Test
	public final void testSameConnection() {

		SailConnection rconn = ((SailRepository)repository).getSail().getConnection();
		SailConnection mconn = memory.getConnection();
		SailConnection sconn = sail.getConnection();

		//		Assert.assert(mconn.getContextIDs(), sconn.getContextIDs());

		ValueFactory vf = sail.getValueFactory();
		IRI sub = vf.createIRI("http://sub_01");
		IRI prp = vf.createIRI("http://prp_01");
		Value obj = vf.createLiteral("obj_01");

		mconn.begin();
		mconn.addStatement(sub, prp, obj);
		mconn.commit();

		sconn.addStatement(prp, sub, obj);
		sconn.removeStatements(prp, sub, obj);

		long msize = mconn.size();
		Assert.assertEquals(1, msize);

		long ssize = sconn.size();
		Assert.assertEquals(1, ssize);

		rconn.close();
		mconn.close();
		sconn.close();
	}

}
