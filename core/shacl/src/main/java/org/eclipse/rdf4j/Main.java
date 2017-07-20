package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.ShaclSail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Main {


	public static void main(String[] args) {

		SailRepository shacl = new SailRepository(new MemoryStore());
		shacl.initialize();

		try (SailRepositoryConnection connection = shacl.getConnection()) {
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			rdfParser.setRDFHandler(new StatementCollector(){
				@Override
				public void handleStatement(Statement st) {
					connection.begin();
					connection.add(st);
					connection.commit();
				}

			});

			String filename = "data.ttl";
			InputStream input = ShaclSail.class.getResourceAsStream("/" + filename);
			rdfParser.parse(input, "");
			RepositoryResult<Statement> result = connection.getStatements(null, null, null);
				while (result.hasNext()) {
					Statement st = result.next();

					System.out.println("db contains: " + st + " : " + st.getPredicate().getLocalName());
				}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ShaclSail shaclSail = new ShaclSail(new MemoryStore(),shacl);
		shaclSail.initialize();
		System.out.println("done");

	}
}
