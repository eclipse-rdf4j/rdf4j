package org.eclipse.rdf4j.sail.lucene.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This unit test reproduces issue #41
 *
 * @author Jacek Grzebyta
 */
public class LuceneIndexLocationTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String luceneIndexPath = "sail-index";

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	Sail sail;

	SailRepository repository;

	RepositoryConnection connection;

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	/**
	 * Set up memory storage located within temporary folder
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		File dataDir = tmpFolder.newFolder();

		sail = new MemoryStore();

		LuceneSail lucene = new LuceneSail();
		lucene.setBaseSail(sail);
		lucene.setParameter(LuceneSail.LUCENE_DIR_KEY, luceneIndexPath);
		lucene.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);

		repository = new SailRepository(lucene);
		repository.setDataDir(dataDir);

		try ( // create temporary transaction to load data
				SailRepositoryConnection cnx = repository.getConnection()) {
			cnx.begin();

			IntStream.rangeClosed(0, 50)
					.forEach(i -> cnx.add(vf.createStatement(vf.createIRI("urn:subject" + i),
							vf.createIRI("urn:predicate:" + i), vf.createLiteral("Value" + i))));
			cnx.commit();
		}
		connection = repository.getConnection();
	}

	@After
	public void tearDown() throws IOException, RepositoryException {
		try {
			if (connection != null) {
				connection.close();
			}
		} finally {
			if (repository != null) {
				repository.shutDown();
			}
		}
	}

	/**
	 * Check Lucene index location
	 *
	 * @throws Exception
	 */
	@Test
	public void IndexLocationTest() throws Exception {
		File dataDir = repository.getDataDir();
		Path lucenePath = repository.getDataDir().toPath().resolve(luceneIndexPath);

		log.info("Lucene index location: {}", lucenePath);
		Assert.assertEquals(dataDir.getAbsolutePath() + File.separator + luceneIndexPath,
				lucenePath.toAbsolutePath().toString());

		Assert.assertTrue(lucenePath.toFile().exists());
		Assert.assertTrue(lucenePath.toFile().isDirectory());
	}

}
