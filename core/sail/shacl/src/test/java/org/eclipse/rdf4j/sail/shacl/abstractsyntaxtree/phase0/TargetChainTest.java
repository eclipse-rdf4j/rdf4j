package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.OrConstraintComponent;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

		assert shapes.get(0) instanceof NodeShape;

		NodeShape nodeShape = (NodeShape) shapes.get(0);

		assert nodeShape.getTargetChain().isOptimizable();

		assert nodeShape.constraintComponent.get(0) instanceof OrConstraintComponent;
		OrConstraintComponent orConstraintComponent = (OrConstraintComponent) nodeShape.constraintComponent.get(0);

		assert orConstraintComponent.getTargetChain().isOptimizable();

		assert orConstraintComponent.getOr().get(0) instanceof PropertyShape;

		assert !orConstraintComponent.getOr().get(0).getTargetChain().isOptimizable();

	}
}
