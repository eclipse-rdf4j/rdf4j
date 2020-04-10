package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;

import java.util.ArrayList;
import java.util.List;

class NodeShape extends Shape implements ConstraintComponent, Identifiable {

	List<ConstraintComponent> constraintComponent = new ArrayList<>();

	public NodeShape() {
	}

	public static NodeShape getInstance(ShaclProperties properties, SailRepositoryConnection connection, Cache cache) {

		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new NodeShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, connection, cache);
		}

		return (NodeShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, SailRepositoryConnection connection, Cache cache) {
		super.populate(properties, connection, cache);

		properties.getProperty()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> PropertyShape.getInstance(p, connection, cache))
				.forEach(constraintComponent::add);

		properties.getNode()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> NodeShape.getInstance(p, connection, cache))
				.forEach(constraintComponent::add);

	}
}
