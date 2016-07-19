package org.eclipse.rdf4j.spin.functions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailQuery;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tester for the property functions.</br>
 * <h2>How does it work?</h2></br>
 * <ul>
 * <li>The functions should be embedded into a SPARQL query.</li>
 * <li>The query might be any type: graph, tuple or boolean.</li>
 * <li>The file with sparql query should be written in <code>src/test/resources/functions/</code>
 * directory.</li>
 * </ul>
 */
@RunWith(Parameterized.class)
public class SpinFunctionTest {

	private static Logger log = LoggerFactory.getLogger(SpinFunctionTest.class);

	private SailRepository repository;

	private SailRepositoryConnection connection;

	public static final String TESTS_ROOT = SpinFunctionTest.class.getResource("/functions/").getFile();

	@Parameters
	public static Collection<Object[]> loadSparqls()
		throws Exception
	{
		List<Object[]> sparqls = new ArrayList<>();

		List<Path> functions = scanTests(Paths.get(TESTS_ROOT));

		for (Path f : functions) {
			File resPath = f.toFile();
			log.info("SPARQL file: '{}'", resPath);
			assert resPath.exists() : "file '" + resPath + "' does not exists";
			sparqls.add(new Object[] { resPath });
		}

		return sparqls;
	}

	/**
	 * Adapted from {@link DirectoryStream} javadoc.
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private static List<Path> scanTests(Path path)
		throws IOException
	{
		log.info("PATH: {}", path);
		List<Path> toReturn = new ArrayList<>();

		try (DirectoryStream<Path> files = Files.newDirectoryStream(path, "*")) {
			for (Path f : files) {
				if (f.toFile().isDirectory()) {
					toReturn.addAll(scanTests(f));
				}
				else {
					log.info("Load test: {}", f);
					toReturn.add(f);
				}
			}
		}

		return toReturn;
	}

	// String content of the loaded SPARQL query
	private String queryString;

	// ... and base URI taken from file path
	private String baseURI;

	public SpinFunctionTest(File resource)
		throws Exception
	{
		log.info("load file: {}", resource);
		baseURI = resource.toURI().toString();
		assert (!baseURI.isEmpty());
		queryString = new String(Files.readAllBytes(resource.toPath()), "utf-8");
		assert StringUtils.isNotBlank(queryString);
		queryString = StringUtils.strip(queryString); // final cleaning
	}

	/**
	 * That might be used as example of usage of {@link TupleFunction}.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		/*
		 * MemoryStore store = new MemoryStore(); SpinSail spinSail = new SpinSail(store);
		 */
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		ForwardChainingRDFSInferencer rdfsInferencer = new ForwardChainingRDFSInferencer(deduper);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		repository = new SailRepository(spinSail);
		repository.initialize();

		connection = repository.getConnection();
		loadRDF("/schema/owl.ttl");
	}

	@Test
	public void runTest()
		throws Exception
	{
		log.info("\nSPARQL query: \n+++++++++++\n{}\n+++++++++++\n", queryString);
		SailQuery query = connection.prepareQuery(QueryLanguage.SPARQL, this.queryString, this.baseURI);

		if (query instanceof BooleanQuery) {
			Assert.assertTrue(((BooleanQuery)query).evaluate());
		}
		else if (query instanceof TupleQuery) {
			Assert.assertFalse("Outcome result is empty",
					Iterations.asList(((TupleQuery)query).evaluate()).isEmpty());
		}
		else if (query instanceof GraphQuery) {
			Assert.assertFalse("Outcome graph is empty",
					Iterations.asList(((GraphQuery)query).evaluate()).isEmpty());
		}
	}

	@After
	public void tearDown()
		// taken from SpifSailTest
		throws RepositoryException
	{
		if (connection != null) {
			connection.close();
		}
		if (repository != null) {
			repository.shutDown();
		}
	}

	/**
	 * Taken from SpifSailTest
	 * 
	 * @param path
	 * @throws IOException
	 */
	private void loadRDF(String path)
		throws IOException
	{
		URL url = getClass().getResource(path);
		InputStream in = url.openStream();
		try {
			connection.add(in, url.toString(), RDFFormat.TURTLE);
		}
		finally {
			in.close();
		}
	}

}
