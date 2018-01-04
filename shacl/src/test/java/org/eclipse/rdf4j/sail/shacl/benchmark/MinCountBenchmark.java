package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class MinCountBenchmark {


	List<List<Statement>> allStatements = new ArrayList<>(10);

	@Setup(Level.Iteration)
	public void setUp() {

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		for(int j = 0; j<10; j++) {
			List<Statement> statements = new ArrayList<>(101);
			allStatements.add(statements);
			for (int i = 0; i < 1000; i++) {
				statements.add(
					vf.createStatement(vf.createIRI("http://example.com/" + i+"_"+j), RDF.TYPE, RDFS.RESOURCE)
				);
				statements.add(
					vf.createStatement(vf.createIRI("http://example.com/" + i+"_"+j), RDFS.LABEL, vf.createLiteral("label" + i))
				);
			}
		}

	}

	@TearDown(Level.Iteration)
	public void tearDown() {
			allStatements.clear();
	}


	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void shacl() {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));

		repository.initialize();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}


	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void noShacl() {

		SailRepository repository = new SailRepository(new MemoryStore());

		repository.initialize();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.commit();
		}
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (List<Statement> statements : allStatements) {
				connection.begin();
				connection.add(statements);
				connection.commit();
			}
		}

	}


}
