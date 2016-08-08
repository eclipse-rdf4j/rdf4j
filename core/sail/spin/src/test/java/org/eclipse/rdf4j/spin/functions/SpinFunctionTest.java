package org.eclipse.rdf4j.spin.functions;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
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

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Unit tester for the property functions.</br>
 * <h2>How does it work?</h2></br>
 * <ul>
 * <li>The functions should be embedded into a SPARQL query.</li>
 * <li>The query might be any type: graph, tuple or boolean.</li>
 * <li>The file with sparql query should be written in <code>src/test/resources/functions/</code>
 * directory.</li>
 * </ul>
 * 
 * @author Jacek Grzebyta
 */
@RunWith(Parameterized.class)
public class SpinFunctionTest {

	private static Logger log = LoggerFactory.getLogger(SpinFunctionTest.class);

	private SailRepository repository;

	private SailRepositoryConnection connection;

	public class TestingCase {

		public Path testFile;

		public Path resultFile;
	}

	public static final String TESTS_ROOT = SpinFunctionTest.class.getResource("/functions/").getFile();

	// Extensions for different file formats
	public static final String SPARQL_EXT = ".rq";

	public static final String SPARQL_RES_EXT = ".srx";

	@Parameters
	public static Collection<Object[]> loadSparqls()
		throws Exception
	{
		List<Object[]> sparqls = new ArrayList<>();

		List<Path> functions = scanTests(Paths.get(TESTS_ROOT));

		Set<String> names = Sets.newHashSet(
				Iterators.transform(functions.iterator(), new Function<Path, String>()
		{

					@Override
					public String apply(Path input) {
						String filename = input.toFile().getAbsolutePath();
						log.info("Process {}", filename);
						return filename.split("\\.")[0];
					}

				}));
		assert (!names.isEmpty()) : "Missing files set";

		for (String f : names) {
			File reqPath = new File(f + SPARQL_EXT);
			log.debug("SPARQL file: '{}'", reqPath);

			// find tuple query result
			File tupleResult = new File(f + SPARQL_RES_EXT);
			log.debug("Tuple result: '{}'", tupleResult);
			if (!tupleResult.exists()) {
				tupleResult = null;
			}

			sparqls.add(new Object[] { reqPath, tupleResult });
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
		log.debug("PATH: {}", path);
		List<Path> toReturn = new ArrayList<>();

		try (DirectoryStream<Path> files = Files.newDirectoryStream(path, "*")) {
			for (Path f : files) {
				if (f.toFile().isDirectory()) {
					toReturn.addAll(scanTests(f));
				}
				else {
					log.debug("Load test: {}", f);
					toReturn.add(f.normalize());
				}
			}
		}

		return toReturn;
	}

	// String content of the loaded SPARQL query
	private String queryString;

	// ... and base URI taken from file path
	private String baseURI;

	private File tupleResult;

	public SpinFunctionTest(File resource, File tupleResult)
		throws Exception
	{
		log.info("load file: {}", resource);
		baseURI = resource.toURI().toString();
		assert (!baseURI.isEmpty());
		queryString = new String(Files.readAllBytes(resource.toPath()), "utf-8");
		assert StringUtils.isNotBlank(queryString);
		queryString = StringUtils.strip(queryString); // final cleaning
		this.tupleResult = tupleResult;
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
		loadRDF("/schema/spif.ttl");
	}

	@Test
	public void runTest()
		throws Exception
	{
		log.debug("\nSPARQL query: \n+++++++++++\n{}\n+++++++++++\n", queryString);
		SailQuery query = connection.prepareQuery(QueryLanguage.SPARQL, this.queryString, this.baseURI);

		if (query instanceof BooleanQuery) {
			Assert.assertTrue(String.format("test <%s> returns false", baseURI),
					((BooleanQuery)query).evaluate());
		}
		else if (query instanceof TupleQuery) {

			/*
			 * //Print result to logger ByteArrayOutputStream out = new ByteArrayOutputStream();
			 * SPARQLResultsXMLWriter writer = new SPARQLResultsXMLWriter(out);
			 * ((TupleQuery)query).evaluate(writer);
			 * log.info("SPARQL response: \n----------\n{}\n---------\n", new String(out.toByteArray()));
			 */

			if (tupleResult == null) {
				throw new RuntimeException(String.format("File does not exists"));
			}
			Assert.assertTrue(assertTupleResults(((TupleQuery)query).evaluate(), tupleResult));

		}
		else {
			throw new RuntimeException(
					String.format("query result format [%s] is not support", query.getClass()));
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

	/**
	 * Run assertion of {@link TupleQueryResult} by comparing with expected result in file.
	 * 
	 * @param tqr
	 * @param expectedResult
	 * @return
	 * @throws Exception
	 */
	protected boolean assertTupleResults(TupleQueryResult tqr, File expectedResult)
		throws Exception
	{
		TupleQueryResult expected = QueryResultIO.parseTuple(new FileInputStream(expectedResult),
				TupleQueryResultFormat.SPARQL);
		return QueryResults.isSubset(tqr, expected);
	}
}
