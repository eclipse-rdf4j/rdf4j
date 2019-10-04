package org.eclipse.rdf4j.workbench;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.solr.SolrUtil;
import org.eclipse.rdf4j.sail.spin.SpinSail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class Temp {

	public static void main(String[] args) throws IOException {

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		NotifyingSail spinSail = new SpinSail(new MemoryStore());
		SailRepository sailRepository = new SailRepository(spinSail);

		System.out.println("init");
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			System.out.println("Loading rules");
			connection.begin();
			connection.add(Temp.class.getClassLoader().getResourceAsStream("rule.ttl"), "", RDFFormat.TURTLE);
			connection.commit();

			print(vf, connection);

			System.out.println("Loading data");

			connection.begin();
			connection.add(Temp.class.getClassLoader().getResourceAsStream("test100.owl"), "", RDFFormat.RDFXML);
			connection.commit();

			print(vf, connection);

			System.out.println("Update age to 10");
			connection.begin();
			String query = IOUtils.toString(Temp.class.getClassLoader().getResourceAsStream("update.rq"),
					StandardCharsets.UTF_8);
			connection.prepareUpdate(query).execute();
			connection.commit();

			print(vf, connection);

			System.out.println("Update age to 19");

			connection.begin();
			String query2 = IOUtils.toString(Temp.class.getClassLoader().getResourceAsStream("update2.rq"),
					StandardCharsets.UTF_8);
			connection.prepareUpdate(query2).execute();
			connection.commit();

			print(vf, connection);

		}

		System.out.println("done");

	}

	private static void print(SimpleValueFactory vf, SailRepositoryConnection connection) {
		System.out.println(
				"##########################################################################################################");
		System.out.println("EXAMPLE DATA");

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(null, null, null, false))) {
			stream.filter(s -> s.getPredicate().toString().startsWith("http://example.org/"))
					.forEach(System.out::println);
		}
		System.out.println(
				"----------------------------------------------------------------------------------------------------------");

		System.out.println("VOLWASSEN");

		try (Stream<Statement> stream = Iterations
				.stream(connection.getStatements(null, vf.createIRI("http://example.org/", "volwassen"), null, true))) {
			stream.forEach(System.out::println);
		}
		System.out.println(
				"----------------------------------------------------------------------------------------------------------");

		System.out.println("\n\n");
	}

}
