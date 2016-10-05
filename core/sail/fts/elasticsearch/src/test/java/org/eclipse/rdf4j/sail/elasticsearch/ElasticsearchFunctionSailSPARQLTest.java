package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.AbstractFunctionLuceneSailSPARQLTest;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailSPARQLTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.elasticsearch.common.io.FileSystemUtils;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchFunctionSailSPARQLTest extends AbstractFunctionLuceneSailSPARQLTest {

	private static final String DATA_DIR = "target/test-data";

	private static final String DATA = "org/eclipse/rdf4j/sail/yeastract_raw.ttl";

	private Logger log = LoggerFactory.getLogger(getClass());

	@After
	public void tearDown()
		throws RepositoryException, IOException
	{
		super.tearDown();
		FileSystemUtils.deleteRecursively(new File(DATA_DIR));
	}

	@Override
	protected void configure(LuceneSail sail) {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_DIR_KEY, DATA_DIR);
		sail.setParameter(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "green");
		sail.setParameter(ElasticsearchIndex.WAIT_FOR_NODES_KEY, ">=1");
	}

	@Override
	protected void populate(RepositoryConnection connection)
		throws Exception
	{
		// process transaction
		try {
			// load resources
			URL resourceURL = AbstractLuceneSailSPARQLTest.class.getClassLoader().getResource(DATA);
			log.info("Resource URL: {}", resourceURL.toString());
			connection.begin();

			assert resourceURL instanceof URL;
			connection.add(resourceURL.openStream(), resourceURL.toString(), RDFFormat.TURTLE,
					new Resource[] {});

		}
		catch (Exception e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.commit();
		}

	}
}
