package org.eclipse.rdf4j.spin.functions;

import java.net.URL;
import java.nio.file.Files;
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
import org.eclipse.rdf4j.repository.sail.SailQuery;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
 * The property functionsunit tester. How does it work?</br>
 * <ul>
 * <li>The functions should be embedded into a SPARQL query.</li>
 * <li>The query might be any type: graph, tuple or boolean.</li>
 * <li>The file with sparql query should be written in <code>src/test/resources/</code> directory.</li>
 * <li>The file pathway should be added into static FINCTIONS variable as presented.</li>
 * </ul>
 * 
 * @author Jacek Grzebyta
 */
@RunWith(Parameterized.class)
public class SpinFunctionTest {

	private static Logger log = LoggerFactory.getLogger(SpinFunctionTest.class);

	private SailRepository repository;

	private SailRepositoryConnection connection;

	public static final String TESTS_ROOT = "/functions/";

	public static final String[] FUNCTIONS = new String[] { "apf/strsplit", "apf/concat" };

	@Parameters
	public static Collection<Object[]> loadSparqls()
		throws Exception
	{
		List<Object[]> sparqls = new ArrayList<>();

		for (String f : FUNCTIONS) {
			String sparqlFile = TESTS_ROOT + f + ".sparql";
			URL resource = SpinFunctionTest.class.getResource(sparqlFile);
			sparqls.add(new Object[] { resource });
		}

		return sparqls;
	}

	// String content of the loaded SPARQL query
	private String queryString;

	// ... and base URI taken from file path
	private String baseURI;

	public SpinFunctionTest(URL resource)
		throws Exception
	{
		log.info("load file: {}", resource);
		baseURI = resource.toURI().toString();
		assert (!baseURI.isEmpty());
		queryString = new String(Files.readAllBytes(Paths.get(resource.toURI())), "utf-8");
		assert StringUtils.isNotBlank(queryString);
		queryString = StringUtils.strip(queryString); // final cleaning
	}

	@Before
	public void setUp()
		throws Exception
	{
		MemoryStore store = new MemoryStore();
		repository = new SailRepository(store);
		repository.initialize();

		connection = repository.getConnection();
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
		throws Exception
	{
		connection.close();
		repository.shutDown();
	}

}
