package org.eclipse.rdf4j.benchmark;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Designed to test the performance of ORDER BY queries.
 * 
 * @see <a href="https://github.com/eclipse/rdf4j/issues/971">https://github.com/eclipse/rdf4j/issues/971</a>
 * @author James Leigh
 */
@Fork(1)
@State(Scope.Thread)
@Warmup(iterations = 2)
@Measurement(iterations = 4)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryOrderBenchmark {

	private File tempDir;

	private Repository repository;

	private RepositoryConnection conn;

	@Param({ "10", "50", "100", "250" })
	public int countk = 10;

	@Param({ "10", "100", "1000", "10000", "50000", "-1" })
	public int limit = 10;

	@Param({ "0", "10000" })
	public int syncThreshold = 10;

	@Setup
	@Before
	public void setup() throws Exception {
		tempDir = File.createTempFile("nativestore", "");
		tempDir.delete();
		tempDir.mkdirs();
		NativeStore sail = new NativeStore(tempDir, "spoc,posc");
		sail.setIterationCacheSyncThreshold(syncThreshold);
		repository = new SailRepository(sail);
		initialize();
		conn = repository.getConnection();
	}

	private void initialize() {
		repository.initialize();
		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			for (int i = 0; i < countk; i++) {
				conn.begin();
				for (int j = 0; j < 1000; j++) {
					IRI subj = vf.createIRI("urn:test:" + Double.toHexString(Math.random()));
					Literal val = vf.createLiteral(Double.toHexString(Math.random()));
					conn.add(subj, RDF.VALUE, val);
				}
				conn.commit();
			}
		}
	}

	@TearDown
	@After
	public void tearDown() throws Exception {
		conn.close();
		repository.shutDown();
		FileUtil.deleteDir(tempDir);
	}

	@Test
	@Benchmark
	public void selectAll() throws Exception {
		StringBuilder rq = new StringBuilder("SELECT * { ?s ?p ?o } ORDER BY ?o");
		if (limit > 0) {
			rq = rq.append(" LIMIT ").append(limit);
		}
		long count = 0;
		try (TupleQueryResult result = conn.prepareTupleQuery(rq.toString()).evaluate()) {
			while (result.hasNext()) {
				result.next();
				count++;
				if (limit > 0 && limit < countk * 1000) {
					assert count <= limit;
				} else {
					assert count <= countk * 1000;
				}
			}
		}
		if (limit > 0 && limit < countk * 1000) {
			assert count == limit;
		} else {
			assert count == countk * 1000;
		}
	}

	@Test
	@Benchmark
	public void selectDistinct() throws Exception {
		StringBuilder rq = new StringBuilder("SELECT DISTINCT ?s ?o { ?s ?p ?o } ORDER BY ?o");
		if (limit > 0) {
			rq = rq.append(" LIMIT ").append(limit);
		}
		long count = 0;
		try (TupleQueryResult result = conn.prepareTupleQuery(rq.toString()).evaluate()) {
			while (result.hasNext()) {
				result.next();
				count++;
				if (limit > 0 && limit < countk * 1000) {
					assert count <= limit;
				} else {
					assert count <= countk * 1000;
				}
			}
		}
		if (limit > 0 && limit < countk * 1000) {
			assert count == limit;
		} else {
			assert count == countk * 1000;
		}
	}

	public static void main(String[] args) throws RunnerException {
		String regexp = ".*" + QueryOrderBenchmark.class.getSimpleName() + ".*";
		new Runner(new OptionsBuilder().include(regexp).build()).run();

	}
}
