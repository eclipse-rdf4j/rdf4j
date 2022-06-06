/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class ClosedConstraintComponent extends AbstractConstraintComponent {

	private final List<Path> paths;
	private final List<IRI> ignoredProperties;

	public ClosedConstraintComponent(ShapeSource shapeSource, List<Resource> property, Resource ignoredProperties) {

		paths = property.stream().flatMap(r -> {
			return shapeSource.getObjects(r, ShapeSource.Predicates.PATH)
					.map(o -> ((Resource) o))
					.map(path -> Path.buildPath(shapeSource, path));

		}).collect(Collectors.toList());

		if (ignoredProperties != null) {
			this.ignoredProperties = ShaclAstLists.toList(shapeSource, ignoredProperties, IRI.class);
		} else {
			this.ignoredProperties = Collections.emptyList();
		}

	}

	public ClosedConstraintComponent(ClosedConstraintComponent closedConstraintComponent) {
		paths = closedConstraintComponent.paths;
		ignoredProperties = closedConstraintComponent.ignoredProperties;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.CLOSED, literal(true));
		// TODO: add ignored properties to model!

	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.ClosedConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new ClosedConstraintComponent(this);
	}
}
