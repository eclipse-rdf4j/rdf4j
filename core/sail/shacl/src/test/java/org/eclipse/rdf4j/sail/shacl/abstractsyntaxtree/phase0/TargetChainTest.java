package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.junit.Test;

public class TargetChainTest {

	@Test
	public void testTargetChain() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclExactly.ttl");

		List<Shape> shapes = shaclSail.refreshShapesPhase0();

		System.out.println();
	}

	@Test
	public void testTargetChainOr() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/or/maxCount/shacl.ttl");

		List<Shape> shapes = shaclSail.refreshShapesPhase0();

		System.out.println();
	}
}
