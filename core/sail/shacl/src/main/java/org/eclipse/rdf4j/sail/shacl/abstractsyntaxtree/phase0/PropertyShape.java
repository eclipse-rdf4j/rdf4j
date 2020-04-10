package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.MinCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.SimplePath;

import java.util.ArrayList;
import java.util.List;

class PropertyShape extends Shape implements ConstraintComponent, Identifiable {

	List<ConstraintComponent> constraintComponent = new ArrayList<>();

	String name;
	String description;
	Object defaultValue;
	Object group;

	Path path;

	public PropertyShape(ShaclProperties properties, SailRepositoryConnection connection) {
		super(properties, connection);

		if (properties.getPath() instanceof IRI) {
			this.path = new SimplePath((IRI) properties.getPath());
		}

		if (properties.getMinCount() != null) {
			constraintComponent.add(new MinCountConstraintComponent(properties.getMinCount()));
		}

	}
}
