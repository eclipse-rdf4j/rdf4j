package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.ShaclSail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Main {


	public static void main(String[] args) throws IOException {

		SailRepository shacl = new SailRepository(new MemoryStore());
		shacl.initialize();

		try (SailRepositoryConnection connection = shacl.getConnection()) {
			String filename = "data.ttl";
			InputStream input = SailRepository.class.getResourceAsStream("/" + filename);
			connection.add(input, "", RDFFormat.TURTLE);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ShaclSail shaclSail = new ShaclSail(new MemoryStore(),shacl);
		shaclSail.initialize();
		SailRepository sailRepository = new SailRepository(shaclSail);
		SailRepositoryConnection sailRepositoryConnection = sailRepository.getConnection();
		RepositoryResult<Statement> result = shacl.getConnection().getStatements(null, null, null);
		while (result.hasNext()) {
			Statement st = result.next();
			sailRepositoryConnection.begin();
			sailRepositoryConnection.add(st);
			sailRepositoryConnection.commit();
		}
		System.out.println("done");

	}

}