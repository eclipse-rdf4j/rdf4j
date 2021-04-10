package org.eclipse.rdf4j.sail.shacl.ast;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.junit.Test;

public class ParsingTest {

	@Test
	public void initialTest() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/datatype/not/shacl.ttl");

		List<Shape> shapes = shaclSail.getCurrentShapes();

		DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();

		shapes.forEach(s -> s.toModel(null, null, emptyModel, new HashSet<>()));

		shaclSail.shutDown();
	}

	@Test
	public void testSplitting() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclExactly.ttl");

		List<Shape> shapes = shaclSail.getCurrentShapes();

		assertEquals(8, shapes.size());

		shapes.forEach(shape -> {
			assertEquals(1, shape.target.size());
			assertEquals(1, shape.constraintComponents.size());

			if (shape.constraintComponents.get(0) instanceof PropertyShape) {
				assertEquals(1, ((PropertyShape) shape.constraintComponents.get(0)).constraintComponents.size());
			}
		});

		shaclSail.shutDown();
	}
}
