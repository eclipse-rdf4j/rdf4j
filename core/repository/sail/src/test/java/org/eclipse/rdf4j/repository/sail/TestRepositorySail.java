package org.eclipse.rdf4j.repository.sail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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
		throws RepositoryConfigException, RepositoryException, IOException
	{
		sail = new RepositorySail(repository);
		sail.setDataDir(dataDir.newFolder());
		sail.initialize();
	}

	@After
	public final void tearDown()
		throws RepositoryException
	{
		sail.shutDown();
	}

	@Test
	public final void testClearAll()
		throws Exception
	{
		SailConnection mconn = memory.getConnection();
		SailConnection sconn = sail.getConnection();

		Assert.assertEquals(0, mconn.size());
		Assert.assertEquals(0, sconn.size());

		mconn.close();
		sconn.close();
	}

	@Test
	public final void testShutdown()
		throws Exception
	{

		memory.shutDown();
		try {
			sail.getConnection();
			Assert.fail("the connection should fail here, bcause the internal sail has been disconnected!");
		}
		catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
		memory.initialize();

		repository.shutDown();
		try {
			sail.getConnection();
			Assert.fail("the connection should fail here, bcause the internal sail has been disconnected!");
		}
		catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
		repository.initialize();

	}

	@Test
	public final void testAddingStatementsToExternalSail()
		throws Exception
	{

		SailConnection mconn = memory.getConnection();
		SailConnection sconn = sail.getConnection();

		Assert.assertEquals(0, sconn.size());

		ValueFactory vf = sail.getValueFactory();
		IRI sub = vf.createIRI("http://sub_01");
		IRI prp = vf.createIRI("http://prp_01");
		Value obj = vf.createLiteral("obj_01");

		sconn.addStatement(sub, prp, obj);

		// try to re-add a statement
		sconn.addStatement(sub, prp, obj);

		// try to re-add the same statement directly to the internal in-memory store
		mconn.begin();
		mconn.addStatement(sub, prp, obj);
		mconn.commit();

		Assert.assertEquals(1, mconn.size());
		Assert.assertEquals(1, sconn.size());

		// check if the statements are the same
		List<Statement> mstatements = iterationToList(mconn.getStatements(null, null, null, false));
		List<Statement> sstatements = iterationToList(mconn.getStatements(null, null, null, false));
		Assert.assertEquals(mstatements, sstatements);

		mconn.close();
		sconn.close();
	}

	private List<Statement> iterationToList(
			CloseableIteration<? extends Statement, ? extends Exception> statements)
		throws Exception
	{
		final List<Statement> list = new ArrayList<>();
		while (statements.hasNext()) {
			list.add(statements.next());
		}
		return list;
	}

	@Test
	public final void testRemovingStatementsFromExternalSail()
		throws Exception
	{
		RepositoryConnection rconn = repository.getConnection();
		SailConnection sconn = sail.getConnection();

		Assert.assertEquals(0, sconn.size());
		Assert.assertEquals(0, rconn.size());

		ValueFactory vf = sail.getValueFactory();
		IRI sub = vf.createIRI("http://sub_01");
		IRI prp = vf.createIRI("http://prp_01");
		Value obj = vf.createLiteral("obj_01");
		Value none = vf.createLiteral("NONE");

		sconn.addStatement(sub, prp, obj);
		sconn.addStatement(prp, sub, none);
		sconn.removeStatements(prp, sub, none);

		Assert.assertEquals(1, sconn.size());
		Assert.assertEquals(1, rconn.size());

		sconn.close();
		rconn.close();
	}

}
