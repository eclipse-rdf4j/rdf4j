package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class ParsingTest {

	@Test
	public void initialTest() throws IOException, NoSuchFieldException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclNodeRecursive.ttl");

		List<Shape> shapes = shaclSail.refreshShapesPhase0();

		System.out.println();

	}
}
