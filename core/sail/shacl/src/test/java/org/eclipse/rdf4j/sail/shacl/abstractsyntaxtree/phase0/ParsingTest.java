package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.model.ModelFactory;
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
	public void initialTest() throws IOException, NoSuchFieldException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/datatype/not/shacl.ttl");

		List<Shape> shapes = shaclSail.refreshShapesPhase0();

		DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();

		shapes.forEach(s -> s.toModel(null, emptyModel, new HashSet<>()));

		emptyModel.setNamespace(SHACL.NS);
		emptyModel.setNamespace(RDF.NS);

		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);

		Rio.write(emptyModel, System.out, RDFFormat.TURTLE, writerConfig);

		System.out.println();

	}

	@Test
	public void testSplitting() throws IOException, NoSuchFieldException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclExactly.ttl");

		List<Shape> shapes = shaclSail.refreshShapesPhase0();

		assertEquals(8, shapes.size());

		shapes.forEach(shape -> {
			assertEquals(1, shape.target.size());
			assertEquals(1, shape.constraintComponent.size());

			if (shape.constraintComponent.get(0) instanceof PropertyShape) {
				assertEquals(1, ((PropertyShape) shape.constraintComponent.get(0)).constraintComponent.size());
			}
		});

	}
}
