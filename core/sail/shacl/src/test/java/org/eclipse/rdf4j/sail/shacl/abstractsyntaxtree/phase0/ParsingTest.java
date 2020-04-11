package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

public class ParsingTest {

	@Test
	public void initialTest() throws IOException, NoSuchFieldException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/or/minCount/shacl.ttl");

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
}
