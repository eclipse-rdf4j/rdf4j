package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;

import java.util.ArrayList;
import java.util.List;

class NodeShape extends Shape implements ConstraintComponent, Identifiable {

	List<ConstraintComponent> constraintComponent = new ArrayList<>();

	public NodeShape(ShaclProperties properties, SailRepositoryConnection connection) {

		super(properties, connection);

		properties.getProperty()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> new PropertyShape(p, connection))
				.forEach(constraintComponent::add);

		System.out.println();

	}
}
