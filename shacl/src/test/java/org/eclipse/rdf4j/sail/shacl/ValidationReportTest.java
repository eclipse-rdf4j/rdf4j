package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static junit.framework.TestCase.assertTrue;

public class ValidationReportTest {

	@Test
	public void simpleFirstTest() throws IOException {
		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} catch (RepositoryException e){
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			Rio.write(actual, System.out, RDFFormat.TURTLE);


			Model expected = Rio.parse(new StringReader("" +
				"@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"_:node1d1e5rk4ux13 a sh:ValidationResult;\n" +
				"  sh:focusNode rdf:subject;\n" +
				"  sh:resultPath rdfs:label;\n" +
				"  sh:sourceConstraintComponent sh:MinCountConstraintComponent;\n" +
				"  sh:sourceShape ex:PersonShapeProperty .\n" +
				"\n" +
				"_:node1d1e5rk4ux14 a sh:ValidationResult;\n" +
				"  sh:focusNode rdfs:Class;\n" +
				"  sh:resultPath rdfs:label;\n" +
				"  sh:sourceConstraintComponent sh:MinCountConstraintComponent;\n" +
				"  sh:sourceShape ex:PersonShapeProperty .\n" +
				"\n" +
				"_:node1d1e5rk4ux12 a sh:ValidationReport;\n" +
				"  sh:conforms false;\n" +
				"  sh:result _:node1d1e5rk4ux13, _:node1d1e5rk4ux14 .\n" +
				""), "", RDFFormat.TURTLE);


			assertTrue(Models.isomorphic(expected, actual));

		}
	}

}
