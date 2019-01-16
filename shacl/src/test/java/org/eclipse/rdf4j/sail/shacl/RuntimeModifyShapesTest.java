package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

import java.io.IOException;

public class RuntimeModifyShapesTest {

	@Test(expected = NoShapesLoadedException.class)
	public void checkForExceptionWhenValidatingWithoutShapes() {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.initialize();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}


	}

	@Test()
	public void checkForNoExceptionWithEmptyTransaction() {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.initialize();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.commit();
		}


	}

	@Test(expected = IllegalStateException.class)
	public void checkForExceptionWhenModifyingShapes() throws IOException {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.initialize();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(RuntimeModifyShapesTest.class.getClassLoader().getResourceAsStream("shaclDatatype.ttl"), "http://example.com/", RDFFormat.TURTLE, ShaclSail.SHAPE_GRAPH);
			connection.commit();
			connection.begin();
			connection.add(RuntimeModifyShapesTest.class.getClassLoader().getResourceAsStream("shacl.ttl"), "http://example.com/", RDFFormat.TURTLE, ShaclSail.SHAPE_GRAPH);
			connection.commit();
		}


	}

}
