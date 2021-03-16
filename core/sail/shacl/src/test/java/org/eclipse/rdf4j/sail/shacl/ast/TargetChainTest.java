package org.eclipse.rdf4j.sail.shacl.ast;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.OrConstraintComponent;
import org.junit.Test;

public class TargetChainTest {

	@Test
	public void testTargetChain() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclExactly.ttl");

		List<Shape> shapes = shaclSail.getCurrentShapes();

		shaclSail.shutDown();
	}

	@Test
	public void testTargetChainOr() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/or/maxCount/shacl.ttl");

		List<Shape> shapes = shaclSail.getCurrentShapes();

		assert shapes.get(0) instanceof NodeShape;

		NodeShape nodeShape = (NodeShape) shapes.get(0);

		assert nodeShape.getTargetChain().isOptimizable();

		assert nodeShape.constraintComponents.get(0) instanceof OrConstraintComponent;
		OrConstraintComponent orConstraintComponent = (OrConstraintComponent) nodeShape.constraintComponents.get(0);

		assert orConstraintComponent.getTargetChain().isOptimizable();

		assert orConstraintComponent.getOr().get(0) instanceof PropertyShape;

		assert !orConstraintComponent.getOr().get(0).getTargetChain().isOptimizable();
		shaclSail.shutDown();

	}
}
