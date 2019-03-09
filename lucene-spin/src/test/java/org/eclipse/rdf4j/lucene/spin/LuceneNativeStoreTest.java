/**
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.lucene.spin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run unit test from {@link AbstractLuceneSailSpinTest} using {@link NativeStore}.
 * 
 * @author Jacek Grzebyta
 * @author Mark Hale
 */
public class LuceneNativeStoreTest extends AbstractLuceneSailSpinTest {

	private static final String DATA = "org/eclipse/rdf4j/sail/220-example.ttl";

	private static Logger log = LoggerFactory.getLogger(LuceneNativeStoreTest.class);

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private Repository repository;

	private RepositoryConnection connection;

	@Override
	public RepositoryConnection getConnection() {
		return connection;
	}

	@Before
	public void setUp()
		throws Exception
	{
		// repository folder
		File tmpDirFolder = tempDir.newFolder();
		log.debug("data file: {}", tmpDirFolder);

		//activate sail debug mode
		// System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
		// load data into native store
		NativeStore store = new NativeStore(tmpDirFolder, "spoc,ospc,posc");

		// add Support for SPIN function
		SpinSail spin = new SpinSail(store);

		// add Lucene Spin Sail support
		LuceneSpinSail luc = new LuceneSpinSail(spin);
		repository = new SailRepository(luc);

		// set up parameters
		configure(luc.getParameters(), store.getDataDir());

		repository.initialize();
		// local connection used only for population
		try (RepositoryConnection localConn = repository.getConnection()) {
			localConn.begin();
			populate(localConn);
			localConn.commit();
		}

		// local connection for verification only
		try (RepositoryConnection localConn = repository.getConnection()) {
			// validate population. Transaction is not required
			//localConn.begin();
			int count = countStatements(localConn);
			log.trace("storage contains {} triples", count);
			Assert.assertTrue(count > 0);
			//localConn.commit();
			localConn.close();
		}

		// testing connection
		connection = repository.getConnection();
		connection.begin();
		Assert.assertTrue("connection is not active", connection.isActive());
	}

	@After
	public void tearDown()
		throws RepositoryException, IOException
	{
		try {
			if (connection != null) {
				connection.close();
			}
		}
		finally {
			if (repository != null) {
				repository.shutDown();
			}
		}
	}

	protected void populate(RepositoryConnection repoConn)
		throws Exception
	{
		// load resources
		assert repoConn.isActive();
		URL resourceURL = LuceneNativeStoreTest.class.getClassLoader().getResource(DATA);
		log.debug("Resource URL: {}", resourceURL.toString());
		Model model = Rio.parse(resourceURL.openStream(), resourceURL.toString(), RDFFormat.TURTLE);
		for (Statement stmt : model) {
			repoConn.add(stmt);
		}
	}

	public void configure(Properties parameters, File store) {
		parameters.setProperty(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
		parameters.setProperty(LuceneSail.LUCENE_DIR_KEY, store.getAbsolutePath() + "/lucene-index");
	}
}
